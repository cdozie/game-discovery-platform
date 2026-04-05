import React, {useEffect, useState} from 'react';
import {
    Avatar,
    Box,
    Button,
    Card,
    CardContent,
    CardMedia,
    Chip,
    Container,
    Divider,
    IconButton,
    InputBase,
    List,
    ListItem,
    ListItemAvatar,
    ListItemText,
    MenuItem,
    Paper,
    Select,
    SelectChangeEvent,
    Stack,
    TextField,
    ToggleButton,
    ToggleButtonGroup,
    Typography
} from '@mui/material';
import {alpha, useTheme} from '@mui/material/styles';
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded';
import StarIcon from '@mui/icons-material/Star';
import DeleteIcon from '@mui/icons-material/Delete';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import {Link as RouterLink, useNavigate} from "react-router-dom";
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {WishlistType} from "../GeneralComponents/types/WishlistType";
import {GameStatusTypes} from "../GeneralComponents/types/GameStatusTypes";
import {formatDisplayDate} from "../GeneralComponents/utils/dates";
import {formatTenPointRating, getRatingVisuals} from "../GeneralComponents/utils/ratings";
import {getErrorMessage} from "../api/client";
import {getWishlist, removeWishlistEntry, updateWishlistRating, updateWishlistStatus} from "../api/games";

type ListDisplayMode = 'list' | 'cards';

const LIST_VIEW_STORAGE_KEY = 'gamesdb:my-list-display-mode';

function splitDisplayValues(rawValue?: string | null): string[] {
    return (rawValue || '')
        .split(',')
        .map(value => value.trim())
        .filter(Boolean);
}

function getStatusVisuals(status: GameStatusTypes) {
    switch (status) {
        case GameStatusTypes.PLAYING:
            return {
                background: 'linear-gradient(135deg, rgba(59, 130, 246, 0.16), rgba(14, 165, 233, 0.22))',
                border: 'rgba(37, 99, 235, 0.28)',
                text: '#1e3a8a'
            };
        case GameStatusTypes.COMPLETED:
            return {
                background: 'linear-gradient(135deg, rgba(34, 197, 94, 0.18), rgba(16, 185, 129, 0.22))',
                border: 'rgba(5, 150, 105, 0.28)',
                text: '#14532d'
            };
        case GameStatusTypes.ON_HOLD:
            return {
                background: 'linear-gradient(135deg, rgba(250, 204, 21, 0.18), rgba(249, 115, 22, 0.2))',
                border: 'rgba(217, 119, 6, 0.28)',
                text: '#854d0e'
            };
        case GameStatusTypes.DROPPED:
            return {
                background: 'linear-gradient(135deg, rgba(248, 113, 113, 0.16), rgba(239, 68, 68, 0.22))',
                border: 'rgba(220, 38, 38, 0.28)',
                text: '#991b1b'
            };
        case GameStatusTypes.PLAN_TO_PLAY:
        default:
            return {
                background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.16), rgba(168, 85, 247, 0.2))',
                border: 'rgba(124, 58, 237, 0.28)',
                text: '#5b21b6'
            };
    }
}

const GameList = () => {
    const theme = useTheme();
    const navigate = useNavigate();
    const [gameList, setGameList] = useState<WishlistType[]>([]);
    const [message, setMessage] = useState('');
    const [severity, setSeverity] = useState(Severity.SUCCESS);
    const [editingGameId, setEditingGameId] = useState<string | null>(null);
    const [editingGameRating, setEditingGameRating] = useState<number | string | null>(null);
    const [editingStatusId, setEditingStatusId] = useState<string | null>(null);
    const [editingGameStatus, setEditingGameStatus] = useState<GameStatusTypes>(GameStatusTypes.PLAN_TO_PLAY);
    const [searchQuery, setSearchQuery] = useState('');
    const [displayMode, setDisplayMode] = useState<ListDisplayMode>(() => {
        if (typeof window === 'undefined') {
            return 'list';
        }
        const savedMode = window.localStorage.getItem(LIST_VIEW_STORAGE_KEY);
        return savedMode === 'cards' ? 'cards' : 'list';
    });

    useEffect(() => {
        getWishlist()
            .then(setGameList)
            .catch(error => {
                setMessage(getErrorMessage(error));
                setSeverity(Severity.ERROR);
            });
    }, []);

    useEffect(() => {
        if (typeof window !== 'undefined') {
            window.localStorage.setItem(LIST_VIEW_STORAGE_KEY, displayMode);
        }
    }, [displayMode]);

    function handleRatingEdit(gameId: string, currentRating: number | string | null) {
        setEditingGameId(gameId);
        setEditingGameRating(currentRating === null || currentRating === undefined ? '' : String(currentRating));
    }

    function handleStatusEdit(gameId: string, currentStatus: GameStatusTypes) {
        setEditingStatusId(gameId);
        setEditingGameStatus(currentStatus);
    }

    function handleRatingChange(event: React.ChangeEvent<HTMLInputElement>) {
        const nextValue = event.target.value;
        if (nextValue === "") {
            setEditingGameRating(nextValue);
            return;
        }

        if (/^\d{0,2}(\.\d{0,1})?$/.test(nextValue)) {
            setEditingGameRating(nextValue);
            return;
        }

        setMessage('Please enter a number between 0 and 10.');
        setSeverity(Severity.ERROR);
    }

    function handleStatusChange(event: SelectChangeEvent<GameStatusTypes>) {
        setEditingGameStatus(event.target.value as GameStatusTypes);
    }

    function handleDisplayModeChange(_event: React.MouseEvent<HTMLElement>, nextDisplayMode: ListDisplayMode | null) {
        if (!nextDisplayMode) {
            return;
        }
        setDisplayMode(nextDisplayMode);
    }

    async function removeFromList(gameId: string) {
        try {
            await removeWishlistEntry(gameId);
            setGameList(previous => previous.filter(game => game.gameinfo.id !== gameId));
            setMessage('Game removed from list.');
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        }
    }

    async function handleRatingSave(gameId: string) {
        try {
            const normalizedRating = editingGameRating === null || editingGameRating === ''
                ? null
                : Math.round(Math.max(0, Math.min(10, Number(editingGameRating))) * 10) / 10;
            if (editingGameRating !== null && editingGameRating !== '' && Number.isNaN(normalizedRating)) {
                setMessage('Please enter a number between 0 and 10.');
                setSeverity(Severity.ERROR);
                return;
            }

            await updateWishlistRating(gameId, normalizedRating);
            setGameList(previous =>
                previous.map(game => game.gameinfo.id === gameId ? {...game, rating: normalizedRating} : game)
            );
            setEditingGameId(null);
            setEditingGameRating(null);
            setMessage('Rating updated.');
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        }
    }

    async function handleStatusSave(gameId: string) {
        try {
            await updateWishlistStatus(gameId, editingGameStatus);
            setGameList(previous =>
                previous.map(game => game.gameinfo.id === gameId ? {...game, status: editingGameStatus} : game)
            );
            setEditingStatusId(null);
            setMessage('Status updated.');
            setSeverity(Severity.SUCCESS);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        }
    }

    function getOrganizedGamesByStatus(list: WishlistType[]): { [status in GameStatusTypes]?: WishlistType[] } {
        return list.reduce((accumulator, game) => {
            const status = game.status;
            if (!accumulator[status]) {
                accumulator[status] = [];
            }
            (accumulator[status] || []).push(game);
            return accumulator;
        }, {} as { [status in GameStatusTypes]?: WishlistType[] });
    }

    function formatDate(dateValue?: string) {
        return formatDisplayDate(dateValue, {
            month: 'short',
            day: 'numeric',
            year: 'numeric'
        }, 'Unknown');
    }

    function renderRatingControl(game: WishlistType, compact = false) {
        const ratingVisuals = getRatingVisuals(game.rating);
        if (editingGameId === game.gameinfo.id) {
            return (
                <Box
                    sx={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 0.9,
                        px: 1.2,
                        py: 0.55,
                        borderRadius: '999px',
                        background: ratingVisuals.background,
                        color: ratingVisuals.text,
                        border: `1px solid ${ratingVisuals.border}`
                    }}
                >
                    <StarIcon fontSize="small" sx={{color: ratingVisuals.icon}}/>
                    <Typography variant="body2" sx={{fontWeight: 700, color: ratingVisuals.text}}>
                        Rating
                    </Typography>
                    <InputBase
                        value={editingGameRating ?? ""}
                        onChange={handleRatingChange}
                        onBlur={() => handleRatingSave(game.gameinfo.id)}
                        onKeyDown={(event) => {
                            if (event.key === 'Enter') {
                                event.preventDefault();
                                handleRatingSave(game.gameinfo.id).catch(() => null);
                            }
                            if (event.key === 'Escape') {
                                setEditingGameId(null);
                                setEditingGameRating(null);
                            }
                        }}
                        autoFocus
                        type="number"
                        inputProps={{min: 0, max: 10, step: 0.1}}
                        sx={{
                            width: compact ? 62 : 58,
                            px: 0.4,
                            py: 0.1,
                            borderRadius: '0.55rem',
                            backgroundColor: 'rgba(255,255,255,0.68)',
                            '& input': {
                                p: 0,
                                textAlign: 'center',
                                fontWeight: 700,
                                color: ratingVisuals.text
                            }
                        }}
                    />
                    <Typography variant="body2" sx={{fontWeight: 700, color: ratingVisuals.text}}>
                        /10
                    </Typography>
                </Box>
            );
        }

        return (
            <Chip
                icon={<StarIcon fontSize="small" sx={{color: ratingVisuals.icon}}/>}
                label={`Rating ${formatTenPointRating(game.rating, '_')}`}
                variant="filled"
                onClick={() => handleRatingEdit(game.gameinfo.id, game.rating)}
                sx={{
                    cursor: 'pointer',
                    background: ratingVisuals.background,
                    color: ratingVisuals.text,
                    border: `1px solid ${ratingVisuals.border}`,
                    '& .MuiChip-label': {
                        color: ratingVisuals.text,
                        fontWeight: 700
                    },
                    '& .MuiChip-icon': {
                        color: ratingVisuals.icon
                    }
                }}
            />
        );
    }

    function renderTrackedPlatformChip(game: WishlistType) {
        const platformLabel = game.selectedPlatform || game.gameinfo.platforms || 'Platform unknown';
        return (
            <Chip
                size="small"
                label={platformLabel}
                variant="filled"
                sx={{
                    fontWeight: 800,
                    letterSpacing: '0.02em',
                    background: 'linear-gradient(135deg, rgba(92, 54, 156, 0.94), rgba(124, 58, 237, 0.92))',
                    color: '#ffffff',
                    border: '1px solid rgba(255,255,255,0.18)',
                    boxShadow: '0 10px 26px rgba(76, 29, 149, 0.18)',
                    '& .MuiChip-label': {
                        color: '#ffffff'
                    }
                }}
            />
        );
    }

    function renderStatusControl(game: WishlistType, compact = false) {
        const statusVisuals = getStatusVisuals(editingStatusId === game.gameinfo.id ? editingGameStatus : game.status);
        if (editingStatusId === game.gameinfo.id) {
            return (
                <Box
                    sx={{
                        display: 'inline-flex',
                        alignItems: 'center',
                        px: 0.9,
                        py: 0.18,
                        borderRadius: '999px',
                        background: compact ? 'rgba(255,255,255,0.96)' : statusVisuals.background,
                        color: statusVisuals.text,
                        border: `1px solid ${statusVisuals.border}`,
                        backdropFilter: compact ? 'blur(12px)' : undefined,
                        boxShadow: compact
                            ? '0 14px 34px rgba(15, 23, 42, 0.26), inset 0 1px 0 rgba(255,255,255,0.72)'
                            : 'inset 0 1px 0 rgba(255,255,255,0.28)'
                    }}
                >
                    <Select
                        value={editingGameStatus}
                        onChange={handleStatusChange}
                        onBlur={() => handleStatusSave(game.gameinfo.id)}
                        autoFocus
                        variant="standard"
                        disableUnderline
                        IconComponent={ExpandMoreRoundedIcon}
                        size="small"
                        sx={{
                            minWidth: compact ? 150 : 168,
                            color: statusVisuals.text,
                            fontWeight: 800,
                            borderRadius: '999px',
                            backgroundColor: compact ? alpha(theme.palette.common.white, 0.78) : 'transparent',
                            '& .MuiSelect-select': {
                                py: 0.55,
                                pr: 3.5,
                                pl: 0.55
                            },
                            '& .MuiSvgIcon-root': {
                                color: statusVisuals.text
                            }
                        }}
                        MenuProps={{
                            PaperProps: {
                                sx: {
                                    borderRadius: '1rem',
                                    mt: 1
                                }
                            }
                        }}
                    >
                        {Object.values(GameStatusTypes).map(statusValue => (
                            <MenuItem key={statusValue} value={statusValue} sx={{fontWeight: 800}}>
                                {statusValue}
                            </MenuItem>
                        ))}
                    </Select>
                </Box>
            );
        }

        return (
            <Chip
                label={game.status || '_'}
                variant="filled"
                onClick={() => handleStatusEdit(game.gameinfo.id, game.status)}
                sx={{
                    cursor: 'pointer',
                    fontWeight: 800,
                    background: compact ? 'rgba(255,255,255,0.9)' : statusVisuals.background,
                    color: statusVisuals.text,
                    border: `1px solid ${statusVisuals.border}`,
                    boxShadow: compact ? '0 12px 28px rgba(15, 23, 42, 0.22)' : undefined,
                    '& .MuiChip-label': {
                        color: statusVisuals.text
                    }
                }}
            />
        );
    }

    function openPublisherSearch(publisher?: string | null) {
        const normalizedPublisher = publisher?.trim();
        if (!normalizedPublisher) {
            return;
        }
        navigate(`/search?publisher=${encodeURIComponent(normalizedPublisher)}&publisherExact=1`);
    }

    function renderListSection(status: string, games: WishlistType[]) {
        return (
            <Box key={status} id={status} sx={{mb: 4}}>
                <Stack direction="row" alignItems="center" spacing={1} sx={{mb: 1.5}}>
                    <Typography variant="h5" fontWeight="bolder">{status}</Typography>
                    <Chip label={`${games.length}`} size="small"/>
                </Stack>
                <List sx={{display: 'grid', gap: 1.5}}>
                    {games.map(game => (
                        <ListItem
                            key={game.id}
                            sx={{
                                alignItems: 'center',
                                gap: 2,
                                pr: 2.25,
                                backgroundColor: 'rgba(255, 255, 255, 0.78)',
                                borderRadius: '1em',
                                transition: 'box-shadow 0.25s ease, transform 0.25s ease',
                                '&:hover': {
                                    boxShadow: '0 20px 40px rgba(31, 9, 60, 0.14)',
                                    transform: 'translateY(-1px)'
                                }
                            }}
                        >
                            <ListItemAvatar>
                                <Avatar src={game.gameinfo.imageUrl} alt={game.gameinfo.name}/>
                            </ListItemAvatar>
                            <ListItemText
                                primary={
                                    <Typography
                                        component={RouterLink}
                                        to={`/games/${game.gameinfo.id}`}
                                        sx={{textDecoration: 'none', color: 'text.primary', fontWeight: 700}}
                                    >
                                        {game.gameinfo.name}
                                    </Typography>
                                }
                                secondaryTypographyProps={{component: 'div'}}
                                secondary={
                                    <Stack spacing={0.75} sx={{mt: 0.75}}>
                                        <Typography variant="body2" color="text.secondary">
                                            {game.gameinfo.provider || 'Unknown publisher'}
                                        </Typography>
                                        <Stack direction="row" spacing={1} useFlexGap alignItems="center" sx={{flexWrap: 'wrap'}}>
                                            {renderTrackedPlatformChip(game)}
                                            {renderRatingControl(game)}
                                        </Stack>
                                    </Stack>
                                }
                            />

                            <Stack
                                direction={{xs: 'column', sm: 'row'}}
                                spacing={1}
                                alignItems="center"
                                justifyContent="flex-end"
                                sx={{ml: 'auto', flexShrink: 0}}
                            >
                                {renderStatusControl(game)}
                                <IconButton edge="end" aria-label="remove" onClick={() => removeFromList(game.gameinfo.id)}>
                                    <DeleteIcon/>
                                </IconButton>
                            </Stack>
                        </ListItem>
                    ))}
                </List>
            </Box>
        );
    }

    function renderCardSection(status: string, games: WishlistType[]) {
        return (
            <Box key={status} id={`${status}-cards`} sx={{mb: 4}}>
                <Stack direction="row" alignItems="center" spacing={1} sx={{mb: 1.5}}>
                    <Typography variant="h5" fontWeight="bolder">{status}</Typography>
                    <Chip label={`${games.length}`} size="small"/>
                </Stack>

                <Box
                    sx={{
                        display: 'grid',
                        gap: 2,
                        gridTemplateColumns: {
                            xs: '1fr',
                            sm: 'repeat(2, minmax(0, 1fr))',
                            xl: 'repeat(3, minmax(0, 1fr))'
                        }
                    }}
                >
                    {games.map(game => (
                        (() => {
                            const catalogPlatforms = splitDisplayValues(game.gameinfo.platforms);
                            const visibleCatalogPlatforms = catalogPlatforms.slice(0, 3);
                            const extraPlatformCount = Math.max(catalogPlatforms.length - visibleCatalogPlatforms.length, 0);
                            const categoryLabels = splitDisplayValues(game.gameinfo.categories).slice(0, 2);

                            return (
                                <Card
                                    key={game.id}
                                    sx={{
                                        position: 'relative',
                                        height: '100%',
                                        display: 'flex',
                                        flexDirection: 'column',
                                        borderRadius: '1.55rem',
                                        overflow: 'hidden',
                                        border: `1px solid ${alpha(theme.palette.primary.main, 0.1)}`,
                                        background: `linear-gradient(180deg, ${alpha(theme.palette.common.white, 0.98)}, ${alpha(theme.palette.primary.light, 0.08)})`,
                                        boxShadow: '0 18px 50px rgba(30, 10, 59, 0.14)',
                                        transition: 'transform 220ms ease, box-shadow 220ms ease',
                                        '&:hover': {
                                            transform: 'translateY(-4px)',
                                            boxShadow: '0 28px 70px rgba(30, 10, 59, 0.22)'
                                        }
                                    }}
                                >
                                    <Box sx={{position: 'relative'}}>
                                        {game.gameinfo.imageUrl ? (
                                            <CardMedia
                                                component="img"
                                                image={game.gameinfo.imageUrl}
                                                alt={game.gameinfo.name}
                                                sx={{height: 220, objectFit: 'cover'}}
                                            />
                                        ) : (
                                            <Box
                                                sx={{
                                                    height: 220,
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'center',
                                                    background: 'linear-gradient(135deg, rgba(69,33,122,0.94), rgba(122,78,184,0.88))',
                                                    color: 'white',
                                                    px: 3
                                                }}
                                            >
                                                <Typography variant="h5" fontWeight={900} textAlign="center">
                                                    {game.gameinfo.name}
                                                </Typography>
                                            </Box>
                                        )}

                                        <Box
                                            sx={{
                                                position: 'absolute',
                                                inset: 0,
                                                background: 'linear-gradient(180deg, rgba(15, 23, 42, 0.08) 0%, rgba(15, 23, 42, 0.18) 45%, rgba(15, 23, 42, 0.72) 100%)',
                                                pointerEvents: 'none'
                                            }}
                                        />

                                        <IconButton
                                            aria-label="remove"
                                            onClick={() => removeFromList(game.gameinfo.id)}
                                            sx={{
                                                position: 'absolute',
                                                top: 14,
                                                right: 14,
                                                backgroundColor: 'rgba(255,255,255,0.9)',
                                                backdropFilter: 'blur(10px)',
                                                '&:hover': {
                                                    backgroundColor: 'rgba(255,255,255,1)'
                                                }
                                            }}
                                        >
                                            <DeleteOutlineIcon/>
                                        </IconButton>

                                        <Stack
                                            spacing={1}
                                            sx={{
                                                position: 'absolute',
                                                left: 16,
                                                right: 16,
                                                bottom: 16
                                            }}
                                        >
                                            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                                                {renderStatusControl(game, true)}
                                                {renderTrackedPlatformChip(game)}
                                            </Stack>
                                        </Stack>
                                    </Box>

                                    <CardContent sx={{display: 'flex', flexDirection: 'column', gap: 1.5, flexGrow: 1, p: 2.2}}>
                                        <Box sx={{minWidth: 0}}>
                                            <Typography
                                                component={RouterLink}
                                                to={`/games/${game.gameinfo.id}`}
                                                variant="h6"
                                                sx={{
                                                    textDecoration: 'none',
                                                    color: 'text.primary',
                                                    fontWeight: 800,
                                                    display: 'block',
                                                    overflowWrap: 'anywhere'
                                                }}
                                            >
                                                {game.gameinfo.name}
                                            </Typography>
                                            <Typography
                                                component="button"
                                                type="button"
                                                variant="body2"
                                                onClick={() => openPublisherSearch(game.gameinfo.provider)}
                                                sx={{
                                                    mt: 0.35,
                                                    p: 0,
                                                    border: 0,
                                                    background: 'transparent',
                                                    textAlign: 'left',
                                                    color: game.gameinfo.provider ? 'text.secondary' : 'text.secondary',
                                                    cursor: game.gameinfo.provider ? 'pointer' : 'default',
                                                    '&:hover': game.gameinfo.provider ? {
                                                        color: theme.palette.primary.dark,
                                                        textDecoration: 'underline'
                                                    } : undefined
                                                }}
                                            >
                                                {game.gameinfo.provider || 'Unknown publisher'}
                                            </Typography>
                                        </Box>

                                        <Box
                                            sx={{
                                                p: 1.35,
                                                borderRadius: '1.1rem',
                                                border: `1px solid ${alpha(theme.palette.primary.main, 0.1)}`,
                                                background: `linear-gradient(180deg, ${alpha(theme.palette.common.white, 0.72)}, ${alpha(theme.palette.primary.light, 0.08)})`
                                            }}
                                        >
                                            <Stack direction={{xs: 'column', sm: 'row'}} spacing={1.5} justifyContent="space-between">
                                                <Box sx={{minWidth: 0}}>
                                                    <Typography variant="caption" sx={{display: 'block', fontWeight: 800, color: 'text.secondary', letterSpacing: '0.08em'}}>
                                                        Release
                                                    </Typography>
                                                    <Typography variant="body2" sx={{mt: 0.35, fontWeight: 700}}>
                                                        {formatDate(game.gameinfo.releaseDate || game.gameinfo.created)}
                                                    </Typography>
                                                </Box>
                                                <Box sx={{minWidth: 0}}>
                                                    <Typography variant="caption" sx={{display: 'block', fontWeight: 800, color: 'text.secondary', letterSpacing: '0.08em'}}>
                                                        Catalog platforms
                                                    </Typography>
                                                    {visibleCatalogPlatforms.length > 0 ? (
                                                        <Stack direction="row" spacing={0.65} flexWrap="wrap" useFlexGap sx={{mt: 0.55}}>
                                                            {visibleCatalogPlatforms.map(platform => (
                                                                <Chip
                                                                    key={platform}
                                                                    label={platform}
                                                                    size="small"
                                                                    variant="filled"
                                                                    sx={{
                                                                        fontWeight: 700,
                                                                        color: '#1d4ed8',
                                                                        border: '1px solid rgba(37, 99, 235, 0.16)',
                                                                        backgroundColor: 'rgba(37, 99, 235, 0.12)',
                                                                        '& .MuiChip-label': {
                                                                            color: '#1d4ed8'
                                                                        }
                                                                    }}
                                                                />
                                                            ))}
                                                            {extraPlatformCount > 0 && (
                                                                <Chip
                                                                    label={`+${extraPlatformCount}`}
                                                                    size="small"
                                                                    variant="filled"
                                                                    sx={{
                                                                        fontWeight: 700,
                                                                        color: '#334155',
                                                                        border: '1px solid rgba(100, 116, 139, 0.16)',
                                                                        backgroundColor: 'rgba(100, 116, 139, 0.12)'
                                                                    }}
                                                                />
                                                            )}
                                                        </Stack>
                                                    ) : (
                                                        <Typography variant="body2" sx={{mt: 0.35, fontWeight: 700}}>
                                                            Unknown
                                                        </Typography>
                                                    )}
                                                </Box>
                                            </Stack>
                                        </Box>

                                        {categoryLabels.length > 0 && (
                                            <Stack direction="row" spacing={0.9} flexWrap="wrap" useFlexGap>
                                                {categoryLabels.map(category => (
                                                    <Chip
                                                        key={category}
                                                        label={category}
                                                        size="small"
                                                        variant="outlined"
                                                        sx={{
                                                            fontWeight: 700,
                                                            color: '#5b2aa8',
                                                            borderColor: 'rgba(91, 42, 168, 0.28)',
                                                            backgroundColor: 'rgba(91, 42, 168, 0.08)'
                                                        }}
                                                    />
                                                ))}
                                            </Stack>
                                        )}

                                        {game.gameinfo.description && (
                                            <Typography
                                                variant="body2"
                                                color="text.secondary"
                                                sx={{
                                                    display: '-webkit-box',
                                                    overflow: 'hidden',
                                                    textOverflow: 'ellipsis',
                                                    WebkitLineClamp: 3,
                                                    WebkitBoxOrient: 'vertical',
                                                    lineHeight: 1.65
                                                }}
                                            >
                                                {game.gameinfo.description}
                                            </Typography>
                                        )}

                                        <Divider sx={{mt: 'auto'}}/>

                                        <Stack direction={{xs: 'column', sm: 'row'}} spacing={1} justifyContent="space-between" alignItems={{xs: 'flex-start', sm: 'center'}}>
                                            {renderRatingControl(game, true)}
                                            <Button
                                                component={RouterLink}
                                                to={`/games/${game.gameinfo.id}`}
                                                variant="contained"
                                                size="small"
                                                endIcon={<OpenInNewIcon/>}
                                            >
                                                Details
                                            </Button>
                                        </Stack>
                                    </CardContent>
                                </Card>
                            );
                        })()
                    ))}
                </Box>
            </Box>
        );
    }

    const normalizedSearchQuery = searchQuery.trim().toLowerCase();
    const filteredGames = normalizedSearchQuery
        ? gameList.filter(game => {
            const haystack = [
                game.gameinfo.name,
                game.gameinfo.provider,
                game.gameinfo.platforms,
                game.gameinfo.categories,
                game.status,
                game.selectedPlatform
            ].filter(Boolean).join(' ').toLowerCase();
            return haystack.includes(normalizedSearchQuery);
        })
        : gameList;
    const groupedGames = getOrganizedGamesByStatus(filteredGames);

    return (
        <Box sx={{width: '100%', pt: {xs: 10, md: 12}, pb: 6}}>
            <Container maxWidth="xl">
                <PopupMessage message={message} severity={severity}/>
                <Stack direction={{xs: 'column', md: 'row'}} justifyContent="space-between" alignItems={{xs: 'flex-start', md: 'center'}} sx={{mb: 3}}>
                    <Box>
                        <Typography variant="h3" fontWeight={900}>My Game List</Typography>
                        <Typography color="text.secondary">
                            Search, rate, organize, and switch between a compact list view or a card wall.
                        </Typography>
                    </Box>
                    <Chip label={`${filteredGames.length} of ${gameList.length} tracked games`} color="secondary"/>
                </Stack>

                <Paper sx={{p: 2.25, borderRadius: '1.4rem', mb: 3}}>
                    <Stack direction={{xs: 'column', lg: 'row'}} spacing={2} alignItems={{xs: 'stretch', lg: 'center'}}>
                        <TextField
                            fullWidth
                            value={searchQuery}
                            onChange={(event) => setSearchQuery(event.target.value)}
                            placeholder="Search your list by title, platform, publisher, category, tracked platform, or status"
                            label="Filter my list"
                        />

                        <Stack spacing={0.75} sx={{minWidth: {lg: 240}}}>
                            <Typography variant="caption" color="text.secondary" sx={{fontWeight: 700, letterSpacing: '0.06em'}}>
                                Display Mode
                            </Typography>
                            <ToggleButtonGroup
                                exclusive
                                color="secondary"
                                value={displayMode}
                                onChange={handleDisplayModeChange}
                                sx={{alignSelf: {xs: 'stretch', lg: 'flex-end'}}}
                            >
                                <ToggleButton value="list" sx={{fontWeight: 700}}>
                                    List
                                </ToggleButton>
                                <ToggleButton value="cards" sx={{fontWeight: 700}}>
                                    Cards
                                </ToggleButton>
                            </ToggleButtonGroup>
                        </Stack>
                    </Stack>
                </Paper>

                <Paper elevation={0} sx={{backgroundColor: 'transparent'}}>
                    {filteredGames.length === 0 ? (
                        <Paper sx={{p: 4, borderRadius: '1.4rem', textAlign: 'center'}}>
                            <Typography variant="h5" fontWeight={800} sx={{mb: 1}}>
                                No list entries match that search.
                            </Typography>
                            <Typography color="text.secondary">
                                Try a different title, platform, publisher, tracked platform, or status filter.
                            </Typography>
                        </Paper>
                    ) : Object.entries(groupedGames).map(([status, games]) => {
                        const statusGames = games ?? [];
                        return displayMode === 'cards'
                            ? renderCardSection(status, statusGames)
                            : renderListSection(status, statusGames);
                    })}
                </Paper>
            </Container>
        </Box>
    );
};

export default GameList;
