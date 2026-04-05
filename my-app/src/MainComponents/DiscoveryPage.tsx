import React, {useCallback, useEffect, useState} from "react";
import {
    Box,
    Button,
    Chip,
    CircularProgress,
    Container,
    Grid,
    Paper,
    Stack,
    Typography
} from "@mui/material";
import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import ExploreOutlinedIcon from "@mui/icons-material/ExploreOutlined";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import ShuffleIcon from "@mui/icons-material/Shuffle";
import {Link as RouterLink, useNavigate} from "react-router-dom";
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {CatalogDiscovery} from "../GeneralComponents/types/CatalogDiscovery";
import {GameData} from "../GeneralComponents/types/GameData";
import {getErrorMessage} from "../api/client";
import {getCatalogDiscovery, removeWishlistEntry, upsertWishlist} from "../api/games";
import {GameStatusTypes} from "../GeneralComponents/types/GameStatusTypes";
import {formatDisplayDate} from "../GeneralComponents/utils/dates";
import ListModificationModal from "./ListModificationModal";
import GameShowcaseCard from "./GameShowcaseCard";
import {requiresPlatformSelection} from "../GeneralComponents/utils/platforms";

function DiscoveryPage() {
    const navigate = useNavigate();
    const [discovery, setDiscovery] = useState<CatalogDiscovery | null>(null);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState('');
    const [severity, setSeverity] = useState<Severity>(Severity.SUCCESS);
    const [modalGameId, setModalGameId] = useState<string | null>(null);

    const loadDiscovery = useCallback(async () => {
        try {
            setLoading(true);
            setDiscovery(await getCatalogDiscovery());
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadDiscovery();
    }, [loadDiscovery]);

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

    function updateGameWishlistFlag(gameId: string, inWishlist: boolean) {
        setDiscovery(previous => {
            if (!previous) {
                return previous;
            }

            const updateList = (list: GameData[]) => list.map(game => game.id === gameId ? {...game, inWishlist} : game);
            const spotlightGame = previous.spotlightGame?.id === gameId
                ? {...previous.spotlightGame, inWishlist}
                : previous.spotlightGame;

            return {
                ...previous,
                spotlightGame,
                featuredGames: updateList(previous.featuredGames),
                newAndNoteworthy: updateList(previous.newAndNoteworthy),
                communityFavorites: updateList(previous.communityFavorites),
                surpriseGames: updateList(previous.surpriseGames)
            };
        });
    }

    function formatDate(dateValue?: string) {
        return formatDisplayDate(dateValue, undefined, "Unknown");
    }

    function renderRail(title: string, icon: React.ReactNode, games: GameData[], badgeFormatter: (game: GameData) => string) {
        return (
            <Box>
                <Stack direction="row" alignItems="center" spacing={1} sx={{mb: 2}}>
                    {icon}
                    <Typography variant="h4" fontWeight={900}>{title}</Typography>
                </Stack>
                <Grid container spacing={3}>
                    {games.map(game => (
                        <Grid item xs={12} md={6} lg={3} key={`${title}-${game.id}`}>
                            <GameShowcaseCard
                                game={game}
                                badgeLabel={badgeFormatter(game)}
                                formatDate={formatDate}
                                onCustomize={setModalGameId}
                                onQuickToggle={(selectedGame) => selectedGame.inWishlist ? removeFromWishlist(selectedGame) : quickPlanToPlay(selectedGame)}
                            />
                        </Grid>
                    ))}
                </Grid>
            </Box>
        );
    }

    function openSpotlightGame() {
        if (!discovery?.spotlightGame?.id) {
            return;
        }
        navigate(`/games/${discovery.spotlightGame.id}`);
    }

    function handleSpotlightKeyDown(event: React.KeyboardEvent<HTMLDivElement>) {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            openSpotlightGame();
        }
    }

    function stopSpotlightEvent(event: React.SyntheticEvent) {
        event.stopPropagation();
    }

    return (
        <Box sx={{width: '100%', pt: {xs: 10, md: 12}, pb: 6}}>
            <PopupMessage message={message} severity={severity}/>
            <Container maxWidth="xl">
                {loading && !discovery && (
                    <Box sx={{display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 360}}>
                        <CircularProgress size={34}/>
                    </Box>
                )}

                {discovery?.spotlightGame && (
                    <Paper
                        role="button"
                        tabIndex={0}
                        aria-label={`Open ${discovery.spotlightGame.name}`}
                        onClick={openSpotlightGame}
                        onKeyDown={handleSpotlightKeyDown}
                        sx={{
                            position: 'relative',
                            p: {xs: 3, md: 5},
                            mb: 4,
                            overflow: 'hidden',
                            borderRadius: '2rem',
                            cursor: 'pointer',
                            color: 'white',
                            backgroundImage: `linear-gradient(115deg, rgba(15,10,45,0.92), rgba(34,19,89,0.78), rgba(24,110,145,0.58)), url(${discovery.spotlightGame.imageUrl})`,
                            backgroundSize: 'cover',
                            backgroundPosition: 'center',
                            boxShadow: '0 30px 90px rgba(16, 8, 42, 0.34)',
                            '&:focus-visible': {
                                outline: '3px solid rgba(255,255,255,0.55)',
                                outlineOffset: '4px'
                            }
                        }}
                    >
                        <Stack direction={{xs: 'column', lg: 'row'}} spacing={4} justifyContent="space-between">
                            <Box
                                maxWidth={760}
                                sx={{
                                    position: 'relative',
                                    zIndex: 1,
                                    p: {xs: 2.5, md: 3.5},
                                    borderRadius: '1.75rem',
                                    background: 'linear-gradient(135deg, rgba(8,10,22,0.78), rgba(17,20,44,0.52))',
                                    border: '1px solid rgba(255,255,255,0.14)',
                                    backdropFilter: 'blur(14px)',
                                    boxShadow: '0 20px 56px rgba(4, 6, 16, 0.28)'
                                }}
                            >
                                <Stack direction="row" spacing={1} alignItems="center" sx={{mb: 1}}>
                                    <AutoAwesomeIcon/>
                                    <Typography variant="overline" sx={{letterSpacing: '0.16em'}}>
                                        Spotlight Pick
                                    </Typography>
                                    {discovery.preferredPlatform && (
                                        <Chip label={`Tuned for ${discovery.preferredPlatform}`} color="secondary" size="small"/>
                                    )}
                                </Stack>
                                <Typography
                                    variant="h2"
                                    fontWeight={900}
                                    sx={{
                                        mb: 1.25,
                                        color: '#fff',
                                        mixBlendMode: {xs: 'normal', md: 'difference'},
                                        textShadow: '0 8px 30px rgba(0,0,0,0.62), 0 2px 12px rgba(0,0,0,0.35)',
                                        lineHeight: 0.96
                                    }}
                                >
                                    {discovery.spotlightGame.name}
                                </Typography>
                                <Typography
                                    sx={{
                                        maxWidth: 680,
                                        color: 'rgba(255,255,255,0.96)',
                                        textShadow: '0 2px 16px rgba(0,0,0,0.35)',
                                        fontSize: {xs: '1rem', md: '1.04rem'},
                                        mb: 2
                                    }}
                                >
                                    {discovery.spotlightGame.description || 'No description available yet.'}
                                </Typography>
                                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{mb: 3}}>
                                    <Chip label={discovery.spotlightGame.provider || 'Unknown publisher'} variant="outlined" sx={{color: 'white', borderColor: 'rgba(255,255,255,0.5)'}}/>
                                    <Chip label={discovery.spotlightGame.platforms || 'Unknown platforms'} variant="outlined" sx={{color: 'white', borderColor: 'rgba(255,255,255,0.5)'}}/>
                                    <Chip label={`${discovery.spotlightGame.wishlistTotal || '0'} wishlists`} variant="outlined" sx={{color: 'white', borderColor: 'rgba(255,255,255,0.5)'}}/>
                                </Stack>
                                <Stack direction={{xs: 'column', sm: 'row'}} spacing={1.5}>
                                    <Button
                                        component={RouterLink}
                                        to={`/games/${discovery.spotlightGame.id}`}
                                        variant="contained"
                                        endIcon={<OpenInNewIcon/>}
                                        onClick={stopSpotlightEvent}
                                    >
                                        Open game page
                                    </Button>
                                    <Button
                                        variant="outlined"
                                        color="inherit"
                                        startIcon={<ShuffleIcon/>}
                                        onClick={(event) => {
                                            stopSpotlightEvent(event);
                                            setModalGameId(discovery.spotlightGame!.id);
                                        }}
                                    >
                                        Add or update list entry
                                    </Button>
                                </Stack>
                            </Box>
                        </Stack>
                    </Paper>
                )}

                {discovery && (
                    <Stack spacing={5}>
                        {renderRail('Featured Now', <ExploreOutlinedIcon color="secondary"/>, discovery.featuredGames, (game) => `${game.interactions || '0'} interactions`)}
                        {renderRail('New And Noteworthy', <AutoAwesomeIcon color="secondary"/>, discovery.newAndNoteworthy, (game) => formatDate(game.created))}
                        {renderRail('Community Favorites', <AutoAwesomeIcon color="secondary"/>, discovery.communityFavorites, (game) => `${game.wishlistTotal || '0'} wishlists`)}
                        {renderRail('Surprise Picks', <ShuffleIcon color="secondary"/>, discovery.surpriseGames, () => 'Random pull')}
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

export default DiscoveryPage;
