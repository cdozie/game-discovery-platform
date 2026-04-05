import React from "react";
import {
    alpha,
    useTheme
} from "@mui/material/styles";
import {
    Box,
    Button,
    Card,
    CardContent,
    CardMedia,
    Chip,
    Stack,
    Typography
} from "@mui/material";
import DeleteOutlineIcon from "@mui/icons-material/DeleteOutline";
import ForumOutlinedIcon from "@mui/icons-material/ForumOutlined";
import OpenInNewIcon from "@mui/icons-material/OpenInNew";
import PlaylistAddIcon from "@mui/icons-material/PlaylistAdd";
import TuneIcon from "@mui/icons-material/Tune";
import {Link as RouterLink, useNavigate} from "react-router-dom";
import {GameData, GameDataWithInteractions} from "../GeneralComponents/types/GameData";

type GameShowcaseCardProps = {
    game: GameData | GameDataWithInteractions;
    badgeLabel?: string;
    onCustomize: (gameId: string) => void;
    onQuickToggle: (game: GameData | GameDataWithInteractions) => void;
    formatDate: (dateValue?: string) => string;
};

function splitDisplayValues(rawValue?: string) {
    return (rawValue || '')
        .split(',')
        .map(value => value.trim())
        .filter(Boolean);
}

function GameShowcaseCard({game, badgeLabel, onCustomize, onQuickToggle, formatDate}: GameShowcaseCardProps) {
    const theme = useTheme();
    const navigate = useNavigate();
    const platformLabels = splitDisplayValues(game.platforms);
    const categoryLabels = splitDisplayValues(game.categories);
    const visiblePlatformLabels = platformLabels.slice(0, 3);
    const extraPlatformCount = Math.max(platformLabels.length - visiblePlatformLabels.length, 0);

    function openGamePage() {
        navigate(`/games/${game.id}`);
    }

    function handleCardKeyDown(event: React.KeyboardEvent<HTMLDivElement>) {
        if (event.key === 'Enter' || event.key === ' ') {
            event.preventDefault();
            openGamePage();
        }
    }

    function stopEvent(event: React.SyntheticEvent) {
        event.stopPropagation();
    }

    function openPublisherSearch(event: React.SyntheticEvent, publisher?: string) {
        stopEvent(event);
        const normalizedPublisher = publisher?.trim();
        if (!normalizedPublisher) {
            return;
        }
        navigate(`/search?publisher=${encodeURIComponent(normalizedPublisher)}&publisherExact=1`);
    }

    return (
        <Card
            role="button"
            tabIndex={0}
            aria-label={`Open ${game.name}`}
            onClick={openGamePage}
            onKeyDown={handleCardKeyDown}
            sx={{
                position: 'relative',
                overflow: 'visible',
                height: '100%',
                borderRadius: '1.4rem',
                cursor: 'pointer',
                background: 'linear-gradient(180deg, rgba(255,255,255,0.96), rgba(243,238,255,0.84))',
                boxShadow: '0 18px 48px rgba(32, 12, 63, 0.18)',
                transition: 'transform 220ms ease, box-shadow 220ms ease',
                '&::before': {
                    content: '""',
                    position: 'absolute',
                    inset: '-10px',
                    borderRadius: '1.8rem',
                    background: 'radial-gradient(circle at top, rgba(148, 97, 255, 0.32), rgba(96, 203, 255, 0.12), transparent 72%)',
                    opacity: 0,
                    filter: 'blur(20px)',
                    transition: 'opacity 220ms ease',
                    zIndex: -1
                },
                '&:hover, &:focus-within': {
                    transform: 'translateY(-6px)',
                    boxShadow: '0 28px 70px rgba(30, 10, 59, 0.28)'
                },
                '&:focus-visible': {
                    outline: '3px solid rgba(101, 76, 214, 0.4)',
                    outlineOffset: '4px'
                },
                '&:hover::before, &:focus-within::before': {
                    opacity: 1
                }
            }}
        >
            <CardMedia component="img" image={game.imageUrl} alt={game.name} sx={{height: 230, objectFit: 'cover', borderTopLeftRadius: '1.4rem', borderTopRightRadius: '1.4rem'}}/>
            <CardContent sx={{display: 'flex', flexDirection: 'column', gap: 1.5}}>
                <Stack direction="row" justifyContent="space-between" alignItems="flex-start" spacing={1} sx={{minWidth: 0}}>
                    <Typography variant="h6" fontWeight={800} sx={{minWidth: 0, overflowWrap: 'anywhere'}}>
                        {game.name}
                    </Typography>
                    {badgeLabel && (
                        <Chip
                            label={badgeLabel}
                            color="secondary"
                            size="small"
                            sx={{maxWidth: '48%', '& .MuiChip-label': {display: 'block', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap'}}}
                        />
                    )}
                </Stack>

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
                                Publisher
                            </Typography>
                            <Typography
                                component="button"
                                type="button"
                                variant="body2"
                                onClick={(event) => openPublisherSearch(event, game.provider)}
                                sx={{
                                    mt: 0.35,
                                    p: 0,
                                    border: 0,
                                    background: 'transparent',
                                    textAlign: 'left',
                                    fontWeight: 700,
                                    overflowWrap: 'anywhere',
                                    color: game.provider ? '#334155' : 'text.primary',
                                    cursor: game.provider ? 'pointer' : 'default',
                                    textDecoration: 'none',
                                    '&:hover': game.provider ? {
                                        color: theme.palette.primary.dark,
                                        textDecoration: 'underline'
                                    } : undefined
                                }}
                            >
                                {game.provider || 'Unknown publisher'}
                            </Typography>
                        </Box>
                        <Box sx={{minWidth: 0}}>
                            <Typography variant="caption" sx={{display: 'block', fontWeight: 800, color: 'text.secondary', letterSpacing: '0.08em'}}>
                                Release
                            </Typography>
                            <Typography variant="body2" sx={{mt: 0.35, fontWeight: 700}}>
                                {formatDate(game.releaseDate || game.created)}
                            </Typography>
                        </Box>
                    </Stack>
                </Box>

                <Box>
                    <Typography variant="caption" sx={{display: 'block', fontWeight: 800, color: 'text.secondary', letterSpacing: '0.08em'}}>
                        Platforms
                    </Typography>
                    {visiblePlatformLabels.length > 0 ? (
                        <Stack direction="row" spacing={0.9} flexWrap="wrap" useFlexGap sx={{mt: 0.8}}>
                            {visiblePlatformLabels.map(platform => (
                                <Chip
                                    key={platform}
                                    label={platform}
                                    size="small"
                                    sx={{
                                        fontWeight: 800,
                                        color: alpha(theme.palette.warning.dark, 0.95),
                                        backgroundColor: alpha(theme.palette.warning.main, 0.14)
                                    }}
                                />
                            ))}
                            {extraPlatformCount > 0 && (
                                <Chip
                                    label={`+${extraPlatformCount} more`}
                                    size="small"
                                    variant="outlined"
                                    sx={{fontWeight: 700}}
                                />
                            )}
                        </Stack>
                    ) : (
                        <Typography variant="body2" color="text.secondary" sx={{mt: 0.7}}>
                            Platform unknown
                        </Typography>
                    )}
                </Box>

                {categoryLabels.length > 0 && (
                    <Stack direction="row" spacing={0.9} flexWrap="wrap" useFlexGap>
                        {categoryLabels.slice(0, 2).map(category => (
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

                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                    <Chip
                        label={`${game.wishlistTotal || '0'} wishlists`}
                        size="small"
                        variant="outlined"
                        sx={{
                            fontWeight: 700,
                            borderColor: alpha(theme.palette.primary.main, 0.18),
                            backgroundColor: alpha(theme.palette.primary.light, 0.06)
                        }}
                    />
                    <Chip
                        icon={<ForumOutlinedIcon fontSize="small"/>}
                        label={`${game.comments || '0'} comments`}
                        size="small"
                        variant="outlined"
                        sx={{
                            fontWeight: 700,
                            borderColor: alpha(theme.palette.info.main, 0.2),
                            backgroundColor: alpha(theme.palette.info.light, 0.08)
                        }}
                    />
                </Stack>

                <Box sx={{display: 'flex', gap: 1, flexWrap: 'wrap', mt: 'auto', pt: 0.5}}>
                    <Button
                        component={RouterLink}
                        to={`/games/${game.id}`}
                        variant="contained"
                        endIcon={<OpenInNewIcon/>}
                        onClick={stopEvent}
                    >
                        Details
                    </Button>
                    <Button
                        onClick={(event) => {
                            stopEvent(event);
                            onCustomize(game.id);
                        }}
                        variant="outlined"
                        startIcon={<TuneIcon/>}
                    >
                        {game.inWishlist ? 'Update' : 'Add'}
                    </Button>
                    <Button
                        onClick={(event) => {
                            stopEvent(event);
                            onQuickToggle(game);
                        }}
                        variant="text"
                        startIcon={game.inWishlist ? <DeleteOutlineIcon/> : <PlaylistAddIcon/>}
                    >
                        {game.inWishlist ? 'Remove' : 'Quick Add'}
                    </Button>
                </Box>
            </CardContent>
        </Card>
    );
}

export default React.memo(GameShowcaseCard);
