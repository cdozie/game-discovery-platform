import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import {
    Autocomplete,
    Button,
    Box,
    Chip,
    CircularProgress,
    Container,
    FormControlLabel,
    Grid,
    InputAdornment,
    MenuItem,
    Paper,
    Stack,
    Switch,
    TextField,
    Typography
} from "@mui/material";
import AddCircleOutlineIcon from "@mui/icons-material/AddCircleOutline";
import SearchIcon from "@mui/icons-material/Search";
import {useNavigate, useSearchParams} from "react-router-dom";
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {GameData} from "../GeneralComponents/types/GameData";
import {GameSearchMetadata} from "../GeneralComponents/types/GameSearchMetadata";
import {getErrorMessage} from "../api/client";
import {getGameSearchMetadata, removeWishlistEntry, searchGamesPage, upsertWishlist} from "../api/games";
import {GameStatusTypes} from "../GeneralComponents/types/GameStatusTypes";
import {formatDisplayDate} from "../GeneralComponents/utils/dates";
import GameShowcaseCard from "./GameShowcaseCard";
import ListModificationModal from "./ListModificationModal";
import {COMMON_PLATFORM_OPTIONS, extractPlatformOptions, requiresPlatformSelection} from "../GeneralComponents/utils/platforms";

const PAGE_SIZE = 20;
const PLATFORM_FILTER_OPTIONS = COMMON_PLATFORM_OPTIONS;
const CATEGORY_FILTER_OPTIONS = [
    'Full Game',
    'Bundle',
    'Add-On',
    'Demo',
    'Subscription',
    'Adventure',
    'Role-playing (RPG)',
    'Shooter',
    'Strategy',
    'Puzzle',
    'Indie',
    'Arcade'
];
const CATALOG_SOURCE_OPTIONS = [
    {value: '', label: 'All catalogs'},
    {value: 'PLAYSTATION_STORE', label: 'PlayStation'},
    {value: 'IGDB_PC_XBOX', label: 'PC / Xbox'},
    {value: 'USER_SUBMITTED', label: 'User submitted'}
];
const SORT_OPTIONS = [
    {value: 'RELEVANCE', label: 'Best match'},
    {value: 'NEWEST', label: 'Newest added'},
    {value: 'RECENTLY_UPDATED', label: 'Recently updated'},
    {value: 'POPULAR', label: 'Most played'},
    {value: 'WISHLIST', label: 'Most wishlisted'},
    {value: 'ALPHABETICAL', label: 'A to Z'}
];

type FacetItem = {
    value: string;
    label: string;
    count: number;
};

function splitDelimitedValues(rawValue?: string | null): string[] {
    if (!rawValue) {
        return [];
    }

    return rawValue
        .split(',')
        .map(value => value.trim())
        .filter(Boolean);
}

function buildFacetItems(values: string[], labelMap?: Map<string, string>, limit = 10): FacetItem[] {
    const counts = new Map<string, number>();
    values.forEach(value => counts.set(value, (counts.get(value) || 0) + 1));
    return Array.from(counts.entries())
        .map(([value, count]) => ({
            value,
            label: labelMap?.get(value) || value,
            count
        }))
        .sort((left, right) => {
            if (right.count !== left.count) {
                return right.count - left.count;
            }
            return left.label.localeCompare(right.label);
        })
        .slice(0, limit);
}

function SearchPage() {
    const navigate = useNavigate();
    const [searchParams, setSearchParams] = useSearchParams();
    const initialQuery = searchParams.get('q') || '';
    const initialPlatform = searchParams.get('platform') || '';
    const initialCategory = searchParams.get('category') || '';
    const initialPublisher = searchParams.get('publisher') || '';
    const initialPublisherExact = searchParams.get('publisherExact') === '1';
    const initialCatalogSource = searchParams.get('source') || '';
    const initialSort = searchParams.get('sort') || 'RELEVANCE';
    const initialHasDescription = searchParams.get('hasDescription') === '1';
    const initialHasImage = searchParams.get('hasImage') === '1';
    const [searchInput, setSearchInput] = useState(initialQuery);
    const [activeQuery, setActiveQuery] = useState(initialQuery);
    const [platformFilter, setPlatformFilter] = useState(initialPlatform);
    const [categoryFilter, setCategoryFilter] = useState(initialCategory);
    const [publisherFilter, setPublisherFilter] = useState(initialPublisher);
    const [publisherExactFilter, setPublisherExactFilter] = useState(initialPublisherExact);
    const [platformInputValue, setPlatformInputValue] = useState(initialPlatform);
    const [categoryInputValue, setCategoryInputValue] = useState(initialCategory);
    const [publisherInputValue, setPublisherInputValue] = useState(initialPublisher);
    const [catalogSourceFilter, setCatalogSourceFilter] = useState(initialCatalogSource);
    const [sortFilter, setSortFilter] = useState(initialSort);
    const [hasDescriptionFilter, setHasDescriptionFilter] = useState(initialHasDescription);
    const [hasImageFilter, setHasImageFilter] = useState(initialHasImage);
    const [results, setResults] = useState<GameData[]>([]);
    const [nextOffset, setNextOffset] = useState(0);
    const [hasMore, setHasMore] = useState(false);
    const [loadingInitial, setLoadingInitial] = useState(false);
    const [loadingMore, setLoadingMore] = useState(false);
    const [message, setMessage] = useState('');
    const [severity, setSeverity] = useState<Severity>(Severity.SUCCESS);
    const [modalGameId, setModalGameId] = useState<string | null>(null);
    const [searchMetadata, setSearchMetadata] = useState<GameSearchMetadata>({
        publishers: [],
        platforms: [],
        categories: [],
        catalogSources: []
    });
    const sentinelRef = useRef<HTMLDivElement | null>(null);
    const requestIdRef = useRef(0);
    const catalogSourceLabels = useMemo(() => new Map(
        CATALOG_SOURCE_OPTIONS
            .filter(option => option.value)
            .map(option => [option.value, option.label] as const)
    ), []);

    useEffect(() => {
        const nextQuery = searchParams.get('q') || '';
        const nextPlatform = searchParams.get('platform') || '';
        const nextCategory = searchParams.get('category') || '';
        const nextPublisher = searchParams.get('publisher') || '';
        const nextPublisherExact = searchParams.get('publisherExact') === '1';
        const nextCatalogSource = searchParams.get('source') || '';
        const nextSort = searchParams.get('sort') || 'RELEVANCE';
        const nextHasDescription = searchParams.get('hasDescription') === '1';
        const nextHasImage = searchParams.get('hasImage') === '1';

        setSearchInput(previous => previous === nextQuery ? previous : nextQuery);
        setActiveQuery(previous => previous === nextQuery ? previous : nextQuery);
        setPlatformFilter(previous => previous === nextPlatform ? previous : nextPlatform);
        setCategoryFilter(previous => previous === nextCategory ? previous : nextCategory);
        setPublisherFilter(previous => previous === nextPublisher ? previous : nextPublisher);
        setPublisherExactFilter(previous => previous === nextPublisherExact ? previous : nextPublisherExact);
        setPlatformInputValue(previous => previous === nextPlatform ? previous : nextPlatform);
        setCategoryInputValue(previous => previous === nextCategory ? previous : nextCategory);
        setPublisherInputValue(previous => previous === nextPublisher ? previous : nextPublisher);
        setCatalogSourceFilter(previous => previous === nextCatalogSource ? previous : nextCatalogSource);
        setSortFilter(previous => previous === nextSort ? previous : nextSort);
        setHasDescriptionFilter(previous => previous === nextHasDescription ? previous : nextHasDescription);
        setHasImageFilter(previous => previous === nextHasImage ? previous : nextHasImage);
    }, [searchParams]);

    useEffect(() => {
        const timeoutId = window.setTimeout(() => {
            getGameSearchMetadata({
                publisherQuery: publisherInputValue || undefined,
                platformQuery: platformInputValue || undefined,
                categoryQuery: categoryInputValue || undefined,
                limit: 14
            }).then(setSearchMetadata).catch(() => null);
        }, 180);

        return () => window.clearTimeout(timeoutId);
    }, [categoryInputValue, platformInputValue, publisherInputValue]);

    useEffect(() => {
        const timeoutId = window.setTimeout(() => {
            const normalized = searchInput.trim();
            setActiveQuery(normalized);
            const nextParams: Record<string, string> = {};
            if (normalized) {
                nextParams.q = normalized;
            }
            if (platformFilter) {
                nextParams.platform = platformFilter;
            }
            if (categoryFilter) {
                nextParams.category = categoryFilter;
            }
            if (publisherFilter) {
                nextParams.publisher = publisherFilter;
                if (publisherExactFilter) {
                    nextParams.publisherExact = '1';
                }
            }
            if (catalogSourceFilter) {
                nextParams.source = catalogSourceFilter;
            }
            if (sortFilter && sortFilter !== 'RELEVANCE') {
                nextParams.sort = sortFilter;
            }
            if (hasDescriptionFilter) {
                nextParams.hasDescription = '1';
            }
            if (hasImageFilter) {
                nextParams.hasImage = '1';
            }
            setSearchParams(nextParams, {replace: true});
        }, 250);

        return () => window.clearTimeout(timeoutId);
    }, [categoryFilter, catalogSourceFilter, hasDescriptionFilter, hasImageFilter, platformFilter, publisherExactFilter, publisherFilter, searchInput, setSearchParams, sortFilter]);

    const formatDate = useCallback((dateValue?: string) => {
        return formatDisplayDate(dateValue, {month: 'long', day: 'numeric', year: 'numeric'});
    }, []);

    const updateGameWishlistFlag = useCallback((gameId: string, inWishlist: boolean) => {
        setResults(previous => previous.map(game => game.id === gameId ? {...game, inWishlist} : game));
    }, []);

    const hasActiveFilters = !!platformFilter
        || !!categoryFilter
        || !!publisherFilter
        || !!catalogSourceFilter
        || hasDescriptionFilter
        || hasImageFilter
        || sortFilter !== 'RELEVANCE';
    const canBrowseResults = !!activeQuery || hasActiveFilters;

    const loadPage = useCallback(async (query: string, offset: number, append: boolean) => {
        if (!query && !hasActiveFilters) {
            setResults([]);
            setNextOffset(0);
            setHasMore(false);
            setMessage('');
            return;
        }

        const requestId = ++requestIdRef.current;

        try {
            if (append) {
                setLoadingMore(true);
            } else {
                setLoadingInitial(true);
            }

            const response = await searchGamesPage(query, PAGE_SIZE, offset, {
                platform: platformFilter || undefined,
                category: categoryFilter || undefined,
                publisher: publisherFilter || undefined,
                publisherExact: publisherExactFilter || undefined,
                catalogSource: catalogSourceFilter || undefined,
                hasDescription: hasDescriptionFilter || undefined,
                hasImage: hasImageFilter || undefined,
                sort: sortFilter || undefined
            });
            if (requestId !== requestIdRef.current) {
                return;
            }

            setResults(previous => append ? [...previous, ...response.items] : response.items);
            setNextOffset(response.offset + response.items.length);
            setHasMore(response.hasMore);
            setMessage(!append && response.items.length === 0 ? 'No games matched the current search and filters.' : '');
            setSeverity(Severity.INFO);
        } catch (error) {
            if (requestId !== requestIdRef.current) {
                return;
            }
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            if (requestId === requestIdRef.current) {
                setLoadingInitial(false);
                setLoadingMore(false);
            }
        }
    }, [categoryFilter, catalogSourceFilter, hasActiveFilters, hasDescriptionFilter, hasImageFilter, platformFilter, publisherExactFilter, publisherFilter, sortFilter]);

    useEffect(() => {
        loadPage(activeQuery, 0, false);
    }, [activeQuery, loadPage]);

    useEffect(() => {
        if (!sentinelRef.current || !canBrowseResults || !hasMore || loadingInitial || loadingMore) {
            return;
        }

        const observer = new IntersectionObserver(entries => {
            if (entries.some(entry => entry.isIntersecting)) {
                loadPage(activeQuery, nextOffset, true);
            }
        }, {rootMargin: '280px'});

        observer.observe(sentinelRef.current);
        return () => observer.disconnect();
    }, [activeQuery, canBrowseResults, hasMore, loadPage, loadingInitial, loadingMore, nextOffset]);

    async function quickPlanToPlay(game: GameData) {
        if (requiresPlatformSelection(game.platforms)) {
            setModalGameId(game.id);
            setMessage('Choose a platform for this game before adding it to your list.');
            setSeverity(Severity.INFO);
            return;
        }

        try {
            await upsertWishlist(game.id, GameStatusTypes.PLAN_TO_PLAY, null);
            updateGameWishlistFlag(game.id, true);
            setMessage(`${game.name} added to your list.`);
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        }
    }

    async function removeFromWishlist(game: GameData) {
        try {
            await removeWishlistEntry(game.id);
            updateGameWishlistFlag(game.id, false);
            setMessage(`${game.name} removed from your list.`);
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        }
    }

    const helperText = useMemo(() => {
        const activeFilterCount = [platformFilter, categoryFilter, publisherFilter, catalogSourceFilter].filter(Boolean).length
            + (hasDescriptionFilter ? 1 : 0)
            + (hasImageFilter ? 1 : 0)
            + (sortFilter !== 'RELEVANCE' ? 1 : 0);
        if (!activeQuery && !hasActiveFilters) {
            return 'Search by title, publisher, platform, genre, or descriptive keywords.';
        }
        if (loadingInitial) {
            return 'Searching the catalog...';
        }
        return `${results.length} result${results.length === 1 ? '' : 's'} loaded${hasMore ? ' so far' : ''}${activeFilterCount ? ` with ${activeFilterCount} filter${activeFilterCount === 1 ? '' : 's'} active` : ''}.`;
    }, [activeQuery, catalogSourceFilter, categoryFilter, hasActiveFilters, hasDescriptionFilter, hasImageFilter, hasMore, loadingInitial, platformFilter, publisherFilter, results.length, sortFilter]);

    const facetSnapshot = useMemo(() => ({
        platforms: buildFacetItems(results.flatMap(game => extractPlatformOptions(game.platforms))),
        categories: buildFacetItems(results.flatMap(game => splitDelimitedValues(game.categories))),
        publishers: buildFacetItems(results.flatMap(game => game.provider ? [game.provider] : []), undefined, 12),
        catalogSources: buildFacetItems(results.flatMap(game => splitDelimitedValues(game.catalogSources)), catalogSourceLabels, 6)
    }), [catalogSourceLabels, results]);

    const platformAutocompleteOptions = useMemo(() => Array.from(new Set([
        ...searchMetadata.platforms.map(item => item.value),
        ...PLATFORM_FILTER_OPTIONS
    ])), [searchMetadata.platforms]);

    const categoryAutocompleteOptions = useMemo(() => Array.from(new Set([
        ...searchMetadata.categories.map(item => item.value),
        ...CATEGORY_FILTER_OPTIONS
    ])), [searchMetadata.categories]);

    const publisherAutocompleteOptions = useMemo(() => searchMetadata.publishers.map(item => item.value), [searchMetadata.publishers]);

    function resetFilters() {
        setPlatformFilter('');
        setPlatformInputValue('');
        setCategoryFilter('');
        setCategoryInputValue('');
        setPublisherFilter('');
        setPublisherInputValue('');
        setPublisherExactFilter(false);
        setCatalogSourceFilter('');
        setSortFilter('RELEVANCE');
        setHasDescriptionFilter(false);
        setHasImageFilter(false);
    }

    function renderFacetGroup(title: string,
                              items: FacetItem[],
                              activeValue: string,
                              onToggle: (nextValue: string) => void,
                              emptyLabel: string) {
        return (
            <Stack spacing={1}>
                <Typography variant="body2" color="text.secondary" sx={{fontWeight: 700}}>
                    {title}
                </Typography>
                {items.length === 0 ? (
                    <Typography variant="body2" color="text.secondary">
                        {emptyLabel}
                    </Typography>
                ) : (
                    <Stack direction="row" spacing={1} useFlexGap sx={{flexWrap: 'wrap'}}>
                        {items.map(item => {
                            const selected = activeValue === item.value;
                            return (
                                <Chip
                                    key={`${title}:${item.value}`}
                                    label={`${item.label} (${item.count})`}
                                    color={selected ? 'secondary' : 'default'}
                                    variant={selected ? 'filled' : 'outlined'}
                                    onClick={() => onToggle(selected ? '' : item.value)}
                                    sx={{fontWeight: selected ? 700 : 500}}
                                />
                            );
                        })}
                    </Stack>
                )}
            </Stack>
        );
    }

    return (
        <Box sx={{width: '100%', pt: {xs: 10, md: 12}, pb: 6}}>
            <PopupMessage message={message} severity={severity}/>
            <Container maxWidth="xl">
                <Paper
                    sx={{
                        p: {xs: 3, md: 4},
                        mb: 4,
                        borderRadius: '2rem',
                        color: 'white',
                        background: 'linear-gradient(135deg, rgba(14,16,34,0.96), rgba(37,61,116,0.9), rgba(23,132,139,0.78))',
                        boxShadow: '0 28px 80px rgba(14, 11, 34, 0.22)'
                    }}
                >
                    <Stack spacing={2}>
                        <Stack direction={{xs: 'column', lg: 'row'}} spacing={2} justifyContent="space-between">
                            <Box>
                                <Typography variant="overline" sx={{letterSpacing: '0.18em', opacity: 0.84}}>
                                    Catalog Search
                                </Typography>
                                <Typography variant="h3" fontWeight={900}>
                                    Browse the full catalog with infinite scroll.
                                </Typography>
                                <Typography sx={{maxWidth: 760, opacity: 0.92}}>
                                    Search by title, publisher, platform, genre, or descriptive keywords. Refine with catalog, content, and quality filters while results continue loading.
                                </Typography>
                            </Box>
                            <Box>
                                <Button
                                    variant="outlined"
                                    startIcon={<AddCircleOutlineIcon/>}
                                    onClick={() => navigate('/submit-game')}
                                    sx={{borderColor: 'rgba(255,255,255,0.48)', color: 'white'}}
                                >
                                    Suggest a missing game
                                </Button>
                            </Box>
                        </Stack>
                        <TextField
                            value={searchInput}
                            onChange={(event) => setSearchInput(event.target.value)}
                            placeholder="Try God of War, PS5, or Sony Interactive"
                            fullWidth
                            autoFocus
                            InputProps={{
                                startAdornment: (
                                    <InputAdornment position="start">
                                        <SearchIcon/>
                                    </InputAdornment>
                                )
                            }}
                            sx={{
                                mt: 1,
                                '& .MuiOutlinedInput-root': {
                                    borderRadius: '1rem',
                                    backgroundColor: 'rgba(255,255,255,0.94)'
                                }
                            }}
                            helperText={helperText}
                            FormHelperTextProps={{sx: {color: 'rgba(255,255,255,0.84)', ml: 0}}}
                        />

                        <Stack direction={{xs: 'column', md: 'row'}} spacing={2}>
                            <TextField
                                select
                                label="Catalog"
                                value={catalogSourceFilter}
                                onChange={(event) => setCatalogSourceFilter(event.target.value)}
                                fullWidth
                                sx={{
                                    '& .MuiOutlinedInput-root': {
                                        borderRadius: '1rem',
                                        backgroundColor: 'rgba(255,255,255,0.94)'
                                    }
                                }}
                            >
                                {CATALOG_SOURCE_OPTIONS.map(option => (
                                    <MenuItem key={option.value || 'all'} value={option.value}>
                                        {option.label}
                                    </MenuItem>
                                ))}
                            </TextField>

                            <Autocomplete
                                freeSolo
                                options={platformAutocompleteOptions}
                                value={platformFilter || null}
                                inputValue={platformInputValue}
                                onInputChange={(_event, value) => {
                                    setPlatformInputValue(value);
                                    setPlatformFilter(value.trim());
                                }}
                                onChange={(_event, value) => {
                                    const nextValue = (typeof value === 'string' ? value : (value || '')).trim();
                                    setPlatformInputValue(nextValue);
                                    setPlatformFilter(nextValue);
                                }}
                                renderInput={(params) => (
                                    <TextField
                                        {...params}
                                        label="Platform"
                                        placeholder="PS5, PC, Xbox Series X|S…"
                                        helperText="Pick a suggested platform or type an exact platform label."
                                    />
                                )}
                                fullWidth
                                sx={{
                                    '& .MuiOutlinedInput-root': {
                                        borderRadius: '1rem',
                                        backgroundColor: 'rgba(255,255,255,0.94)'
                                    }
                                }}
                            />

                            <Autocomplete
                                freeSolo
                                options={categoryAutocompleteOptions}
                                value={categoryFilter || null}
                                inputValue={categoryInputValue}
                                onInputChange={(_event, value) => {
                                    setCategoryInputValue(value);
                                    setCategoryFilter(value.trim());
                                }}
                                onChange={(_event, value) => {
                                    const nextValue = (typeof value === 'string' ? value : (value || '')).trim();
                                    setCategoryInputValue(nextValue);
                                    setCategoryFilter(nextValue);
                                }}
                                renderInput={(params) => (
                                    <TextField
                                        {...params}
                                        label="Category / Genre"
                                        placeholder="Adventure, RPG, Demo…"
                                        helperText="Suggested from indexed catalog categories."
                                    />
                                )}
                                fullWidth
                                sx={{
                                    '& .MuiOutlinedInput-root': {
                                        borderRadius: '1rem',
                                        backgroundColor: 'rgba(255,255,255,0.94)'
                                    }
                                }}
                            />

                            <Autocomplete
                                freeSolo
                                options={publisherAutocompleteOptions}
                                value={publisherFilter || null}
                                inputValue={publisherInputValue}
                                onInputChange={(_event, value) => {
                                    setPublisherInputValue(value);
                                    setPublisherFilter(value.trim());
                                    setPublisherExactFilter(false);
                                }}
                                onChange={(_event, value) => {
                                    const nextValue = (typeof value === 'string' ? value : (value || '')).trim();
                                    setPublisherInputValue(nextValue);
                                    setPublisherFilter(nextValue);
                                }}
                                renderInput={(params) => (
                                    <TextField
                                        {...params}
                                        label="Publisher / studio"
                                        placeholder="Sony Interactive, Atlus, Sega..."
                                        helperText={publisherExactFilter ? 'Exact publisher match is active from a publisher link.' : 'Type a publisher or studio name to filter the catalog.'}
                                    />
                                )}
                                sx={{
                                    '& .MuiOutlinedInput-root': {
                                        borderRadius: '1rem',
                                        backgroundColor: 'rgba(255,255,255,0.94)'
                                    }
                                }}
                                fullWidth
                            />

                            <TextField
                                select
                                label="Sort"
                                value={sortFilter}
                                onChange={(event) => setSortFilter(event.target.value)}
                                fullWidth
                                sx={{
                                    '& .MuiOutlinedInput-root': {
                                        borderRadius: '1rem',
                                        backgroundColor: 'rgba(255,255,255,0.94)'
                                    }
                                }}
                            >
                                {SORT_OPTIONS.map(option => (
                                    <MenuItem key={option.value} value={option.value}>
                                        {option.label}
                                    </MenuItem>
                                ))}
                            </TextField>
                        </Stack>

                        <Stack
                            direction={{xs: 'column', md: 'row'}}
                            spacing={2}
                            alignItems={{xs: 'stretch', md: 'center'}}
                            justifyContent="space-between"
                        >
                            <Stack direction={{xs: 'column', sm: 'row'}} spacing={1}>
                                <FormControlLabel
                                    control={<Switch checked={hasDescriptionFilter} onChange={(event) => setHasDescriptionFilter(event.target.checked)} color="secondary"/>}
                                    label="Only with descriptions"
                                />
                                <FormControlLabel
                                    control={<Switch checked={hasImageFilter} onChange={(event) => setHasImageFilter(event.target.checked)} color="secondary"/>}
                                    label="Only with cover art"
                                />
                            </Stack>

                            <Button
                                variant="outlined"
                                onClick={resetFilters}
                                sx={{
                                    alignSelf: {xs: 'stretch', md: 'center'},
                                    borderRadius: '999px',
                                    borderColor: 'rgba(255,255,255,0.45)',
                                    color: 'white'
                                }}
                            >
                                Reset filters
                            </Button>
                        </Stack>
                    </Stack>
                </Paper>

                {canBrowseResults && results.length > 0 && (
                    <Paper sx={{p: {xs: 2.5, md: 3}, mb: 3, borderRadius: '1.5rem'}}>
                        <Stack spacing={2.25}>
                            <Box>
                                <Typography variant="h6" fontWeight={800}>Facet snapshot</Typography>
                                <Typography color="text.secondary">
                                    Refine the results already loaded on this page with quick platform, publisher, genre, and catalog chips.
                                </Typography>
                            </Box>
                            {renderFacetGroup('Platforms', facetSnapshot.platforms, platformFilter, setPlatformFilter, 'No platform facets yet for these loaded results.')}
                            {renderFacetGroup('Genres / categories', facetSnapshot.categories, categoryFilter, setCategoryFilter, 'No category facets yet for these loaded results.')}
                            {renderFacetGroup('Publishers / studios', facetSnapshot.publishers, publisherFilter, setPublisherFilter, 'No publisher facets yet for these loaded results.')}
                            {renderFacetGroup('Catalog sources', facetSnapshot.catalogSources, catalogSourceFilter, setCatalogSourceFilter, 'No catalog source facets yet for these loaded results.')}
                        </Stack>
                    </Paper>
                )}

                {!canBrowseResults ? (
                    <Paper sx={{p: 4, borderRadius: '1.6rem', textAlign: 'center'}}>
                        <Typography variant="h5" fontWeight={800} sx={{mb: 1}}>
                            Start typing or select a filter to browse the catalog
                        </Typography>
                        <Typography color="text.secondary">
                            Results will stream in page by page as you scroll, even if you start with filters first.
                        </Typography>
                    </Paper>
                ) : (
                    <Stack spacing={3}>
                        {loadingInitial ? (
                            <Box sx={{display: 'flex', justifyContent: 'center', py: 6}}>
                                <CircularProgress size={34}/>
                            </Box>
                        ) : (
                            <>
                                <Grid container spacing={3}>
                                    {results.map(game => (
                                        <Grid item xs={12} md={6} lg={4} xl={3} key={game.id}>
                                            <GameShowcaseCard
                                                game={game}
                                                onCustomize={setModalGameId}
                                                onQuickToggle={(selectedGame) => selectedGame.inWishlist ? removeFromWishlist(selectedGame) : quickPlanToPlay(selectedGame)}
                                                formatDate={formatDate}
                                            />
                                        </Grid>
                                    ))}
                                </Grid>

                                <Box ref={sentinelRef} sx={{display: 'flex', justifyContent: 'center', py: 2}}>
                                    {loadingMore ? <CircularProgress size={28}/> : hasMore ? <Typography color="text.secondary">Scroll for more results</Typography> : results.length > 0 ? <Typography color="text.secondary">You’ve reached the end of the results.</Typography> : null}
                                </Box>
                            </>
                        )}
                    </Stack>
                )}

                {modalGameId && (
                    <ListModificationModal
                        open={!!modalGameId}
                        onClose={() => setModalGameId(null)}
                        gameId={modalGameId}
                        updateGameInWishlist={updateGameWishlistFlag}
                        onActionComplete={(nextMessage, nextSeverity) => {
                            setMessage(nextMessage);
                            setSeverity(nextSeverity);
                        }}
                    />
                )}
            </Container>
        </Box>
    );
}

export default SearchPage;
