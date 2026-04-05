import React, {useEffect, useMemo, useState} from 'react';
import {
    Box,
    Button,
    Chip,
    CircularProgress,
    Divider,
    FormControl,
    InputAdornment,
    InputLabel,
    MenuItem,
    Modal,
    Paper,
    Select,
    Stack,
    TextField,
    Typography
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import ForumOutlinedIcon from "@mui/icons-material/ForumOutlined";
import PlaylistAddIcon from "@mui/icons-material/PlaylistAdd";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import StarOutlineIcon from "@mui/icons-material/StarOutline";
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {ModalMainPropsWithIdAndUpdate} from "../GeneralComponents/types/ModalMainProps";
import {GameStatusTypes} from "../GeneralComponents/types/GameStatusTypes";
import {WishlistType} from "../GeneralComponents/types/WishlistType";
import {getErrorMessage} from "../api/client";
import {getWishlistEntry, removeWishlistEntry, upsertWishlist} from "../api/games";
import {extractPlatformOptions} from "../GeneralComponents/utils/platforms";
import {formatDisplayDate} from "../GeneralComponents/utils/dates";
import {formatTenPointRating, getRatingVisuals} from "../GeneralComponents/utils/ratings";

type ListModificationModalProps = ModalMainPropsWithIdAndUpdate & {
    onActionComplete?: (message: string, severity: Severity) => void;
};

function formatDate(dateValue?: string | null) {
    return formatDisplayDate(dateValue, {month: 'long', day: 'numeric', year: 'numeric'}, 'Release date unknown');
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

const ListModificationModal: React.FC<ListModificationModalProps> = ({
    open,
    onClose,
    gameId,
    updateGameInWishlist,
    onActionComplete
}) => {
    const [message, setMessage] = useState('');
    const [severity, setSeverity] = useState<Severity>(Severity.SUCCESS);
    const [rating, setRating] = useState<number | string>("");
    const [status, setStatus] = useState<GameStatusTypes>(GameStatusTypes.PLAN_TO_PLAY);
    const [selectedPlatform, setSelectedPlatform] = useState('');
    const [wishlistEntry, setWishlistEntry] = useState<WishlistType | null>(null);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [removing, setRemoving] = useState(false);

    useEffect(() => {
        if (!open) {
            return;
        }

        setLoading(true);
        setMessage('');

        getWishlistEntry(gameId)
            .then(response => {
                setWishlistEntry(response);
                setStatus(response.status || GameStatusTypes.PLAN_TO_PLAY);
                setRating(response.rating ?? "");
                const platformOptions = extractPlatformOptions(response.gameinfo.platforms);
                setSelectedPlatform(response.selectedPlatform || (platformOptions.length === 1 ? platformOptions[0] : ''));
            })
            .catch(error => {
                setMessage(getErrorMessage(error));
                setSeverity(Severity.ERROR);
            })
            .finally(() => setLoading(false));
    }, [gameId, open]);

    function handleRatingChange(event: React.ChangeEvent<HTMLInputElement>) {
        const newValue = event.target.value;
        if (newValue === "") {
            setRating("");
            return;
        }

        if (!Number.isNaN(Number(newValue))) {
            const clampedValue = Math.max(0, Math.min(10, Number(newValue)));
            setRating(String(Math.round(clampedValue * 10) / 10));
            return;
        }

        setMessage('Please enter a number between 0 and 10.');
        setSeverity(Severity.ERROR);
    }

    async function handleSubmit() {
        const platformOptions = extractPlatformOptions(wishlistEntry?.gameinfo.platforms);
        if (platformOptions.length > 1 && !selectedPlatform) {
            setMessage('Choose the platform you want this list entry to track.');
            setSeverity(Severity.ERROR);
            return;
        }

        try {
            setSaving(true);
            const wasInWishlist = !!wishlistEntry?.gameinfo.inWishlist;
            await upsertWishlist(gameId, status, rating === "" ? null : rating, selectedPlatform || null);
            updateGameInWishlist(gameId, true);
            onActionComplete?.(
                `${wishlistEntry?.gameinfo.name || 'Game'} ${wasInWishlist ? 'updated in' : 'added to'} your list.`,
                Severity.SUCCESS
            );
            onClose();
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setSaving(false);
        }
    }

    async function handleDeleteClick() {
        try {
            setRemoving(true);
            await removeWishlistEntry(gameId);
            updateGameInWishlist(gameId, false);
            onActionComplete?.(
                `${wishlistEntry?.gameinfo.name || 'Game'} removed from your list.`,
                Severity.SUCCESS
            );
            onClose();
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setRemoving(false);
        }
    }

    const game = wishlistEntry?.gameinfo;
    const alreadyTracked = !!game?.inWishlist;
    const statusLabel = alreadyTracked ? 'Currently in your list' : 'New list entry';
    const ratingSummary = formatTenPointRating(rating, 'No rating set yet');
    const description = useMemo(() => {
        if (!game?.description) {
            return 'Use this panel to save your status, set a score, and decide whether this title belongs in your personal catalog.';
        }

        return game.description;
    }, [game?.description]);
    const platformOptions = useMemo(() => extractPlatformOptions(game?.platforms), [game?.platforms]);
    const ratingVisuals = getRatingVisuals(rating);
    const statusVisuals = getStatusVisuals(status);

    return (
        <Modal
            open={open}
            onClose={onClose}
            aria-labelledby="list-mod-modal"
            sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                backdropFilter: 'blur(8px)',
                p: 2
            }}
        >
            <Paper
                sx={{
                    width: '100%',
                    maxWidth: 860,
                    maxHeight: '92vh',
                    overflowY: 'auto',
                    borderRadius: '2rem',
                    background: 'linear-gradient(180deg, rgba(255,255,255,0.98), rgba(247,243,255,0.96))',
                    boxShadow: '0 40px 100px rgba(19, 10, 43, 0.36)'
                }}
            >
                <PopupMessage message={message} severity={severity}/>

                {loading ? (
                    <Box sx={{display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: 320}}>
                        <CircularProgress/>
                    </Box>
                ) : game ? (
                    <>
                        <Box
                            sx={{
                                position: 'relative',
                                minHeight: {xs: 280, md: 320},
                                p: {xs: 3, md: 4},
                                display: 'flex',
                                alignItems: 'flex-end',
                                backgroundImage: `linear-gradient(180deg, rgba(9,11,28,0.16), rgba(9,11,28,0.88)), url(${game.imageUrl})`,
                                backgroundSize: 'cover',
                                backgroundPosition: 'center'
                            }}
                        >
                            <Box
                                sx={{
                                    maxWidth: 640,
                                    p: {xs: 2.5, md: 3},
                                    borderRadius: '1.5rem',
                                    color: 'white',
                                    background: 'linear-gradient(135deg, rgba(12,14,32,0.84), rgba(27,35,67,0.56))',
                                    backdropFilter: 'blur(14px)',
                                    border: '1px solid rgba(255,255,255,0.12)'
                                }}
                            >
                                <Typography variant="overline" sx={{letterSpacing: '0.16em', opacity: 0.82}}>
                                    {statusLabel}
                                </Typography>
                                <Typography variant="h3" fontWeight={900} sx={{mb: 1}}>
                                    {game.name}
                                </Typography>
                                <Typography sx={{opacity: 0.92, mb: 2}}>
                                    {description}
                                </Typography>
                                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                                    <Chip label={game.provider || 'Unknown publisher'} variant="outlined" sx={{color: 'white', borderColor: 'rgba(255,255,255,0.4)'}}/>
                                    <Chip label={game.platforms || 'Platform unknown'} variant="outlined" sx={{color: 'white', borderColor: 'rgba(255,255,255,0.4)'}}/>
                                    <Chip label={formatDate(game.releaseDate || game.created)} variant="outlined" sx={{color: 'white', borderColor: 'rgba(255,255,255,0.4)'}}/>
                                </Stack>
                            </Box>
                        </Box>

                        <Box sx={{p: {xs: 3, md: 4}}}>
                            <Stack direction={{xs: 'column', lg: 'row'}} spacing={3} alignItems="stretch">
                                <Paper sx={{p: 3, borderRadius: '1.5rem', flex: 1.3, backgroundColor: 'rgba(255,255,255,0.72)'}}>
                                    <Stack spacing={2.5}>
                                        <Box>
                                            <Typography variant="h5" fontWeight={800}>List entry details</Typography>
                                            <Typography color="text.secondary">
                                                Tune the status and score for this game. Updates save directly to your list and refresh the game’s aggregate stats.
                                            </Typography>
                                        </Box>

                                        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                                            <Chip icon={<StarOutlineIcon fontSize="small"/>} label={`Your rating: ${ratingSummary}`} variant="outlined"/>
                                            <Chip icon={<ForumOutlinedIcon fontSize="small"/>} label={`${game.comments || '0'} comments`} variant="outlined"/>
                                            <Chip label={`${game.wishlistTotal || '0'} wishlist entries`} variant="outlined"/>
                                            <Chip label={`Tracked platform: ${selectedPlatform || 'Choose one'}`} variant="outlined"/>
                                            {game.averageRating && <Chip label={`Community avg ${formatTenPointRating(game.averageRating)}`} variant="outlined"/>}
                                        </Stack>

                                        <Stack direction={{xs: 'column', md: 'row'}} spacing={2}>
                                            <Paper
                                                variant="outlined"
                                                sx={{
                                                    flex: 1,
                                                    p: 2,
                                                    borderRadius: '1.25rem',
                                                    background: ratingVisuals.background,
                                                    borderColor: ratingVisuals.border
                                                }}
                                            >
                                                <Stack spacing={1.25}>
                                                    <Typography variant="body2" sx={{fontWeight: 800, color: ratingVisuals.text}}>
                                                        Personal rating
                                                    </Typography>
                                                    <Chip
                                                        icon={<StarOutlineIcon fontSize="small" sx={{color: ratingVisuals.icon}}/>}
                                                        label={`${ratingVisuals.label} · ${ratingSummary}`}
                                                        sx={{
                                                            alignSelf: 'flex-start',
                                                            background: 'rgba(255,255,255,0.66)',
                                                            color: ratingVisuals.text,
                                                            border: `1px solid ${ratingVisuals.border}`,
                                                            '& .MuiChip-label': {fontWeight: 800}
                                                        }}
                                                    />
                                                    <TextField
                                                        value={rating}
                                                        onChange={handleRatingChange}
                                                        type='number'
                                                        variant="outlined"
                                                        fullWidth
                                                        inputProps={{min: 0, max: 10, step: 0.1}}
                                                        helperText="Use a 0 to 10 score. Leave it blank if you don’t want to rate this entry yet."
                                                        InputProps={{
                                                            startAdornment: <InputAdornment position="start">/10</InputAdornment>
                                                        }}
                                                        sx={{
                                                            '& .MuiOutlinedInput-root': {
                                                                borderRadius: '1rem',
                                                                backgroundColor: 'rgba(255,255,255,0.76)'
                                                            }
                                                        }}
                                                    />
                                                </Stack>
                                            </Paper>

                                            <Paper
                                                variant="outlined"
                                                sx={{
                                                    flex: 1,
                                                    p: 2,
                                                    borderRadius: '1.25rem',
                                                    background: statusVisuals.background,
                                                    borderColor: statusVisuals.border
                                                }}
                                            >
                                                <Stack spacing={1.25}>
                                                    <Typography variant="body2" sx={{fontWeight: 800, color: statusVisuals.text}}>
                                                        List status
                                                    </Typography>
                                                    <Chip
                                                        label={status}
                                                        sx={{
                                                            alignSelf: 'flex-start',
                                                            background: 'rgba(255,255,255,0.66)',
                                                            color: statusVisuals.text,
                                                            border: `1px solid ${statusVisuals.border}`,
                                                            '& .MuiChip-label': {fontWeight: 800}
                                                        }}
                                                    />
                                                    <FormControl fullWidth>
                                                        <InputLabel id="list-status-label">Status</InputLabel>
                                                        <Select
                                                            labelId="list-status-label"
                                                            label="Status"
                                                            value={status}
                                                            onChange={(event) => setStatus(event.target.value as GameStatusTypes)}
                                                            fullWidth
                                                            sx={{
                                                                borderRadius: '1rem',
                                                                backgroundColor: 'rgba(255,255,255,0.76)'
                                                            }}
                                                        >
                                                            {Object.values(GameStatusTypes).map(statusValue => (
                                                                <MenuItem key={statusValue} value={statusValue}>
                                                                    {statusValue}
                                                                </MenuItem>
                                                            ))}
                                                        </Select>
                                                    </FormControl>
                                                </Stack>
                                            </Paper>
                                        </Stack>

                                        <Paper
                                            variant="outlined"
                                            sx={{
                                                p: 2,
                                                borderRadius: '1.25rem',
                                                background: 'linear-gradient(135deg, rgba(139, 92, 246, 0.12), rgba(59, 130, 246, 0.14))',
                                                borderColor: 'rgba(91, 33, 182, 0.18)'
                                            }}
                                        >
                                            <Stack spacing={1.25}>
                                                <Typography variant="body2" sx={{fontWeight: 800, color: '#4c1d95'}}>
                                                    Tracked platform
                                                </Typography>
                                                <Chip
                                                    label={selectedPlatform || 'Choose one'}
                                                    sx={{
                                                        alignSelf: 'flex-start',
                                                        background: 'rgba(255,255,255,0.66)',
                                                        color: '#4c1d95',
                                                        border: '1px solid rgba(91, 33, 182, 0.18)',
                                                        '& .MuiChip-label': {fontWeight: 800}
                                                    }}
                                                />
                                                <FormControl fullWidth>
                                                    <InputLabel id="list-platform-label">Tracked platform</InputLabel>
                                                    <Select
                                                        labelId="list-platform-label"
                                                        label="Tracked platform"
                                                        value={selectedPlatform}
                                                        onChange={(event) => setSelectedPlatform(event.target.value)}
                                                        disabled={platformOptions.length <= 1}
                                                        fullWidth
                                                        sx={{
                                                            borderRadius: '1rem',
                                                            backgroundColor: 'rgba(255,255,255,0.76)'
                                                        }}
                                                    >
                                                        {platformOptions.length > 1 && (
                                                            <MenuItem value="">
                                                                Choose platform
                                                            </MenuItem>
                                                        )}
                                                        {platformOptions.map(platformOption => (
                                                            <MenuItem key={platformOption} value={platformOption}>
                                                                {platformOption}
                                                            </MenuItem>
                                                        ))}
                                                    </Select>
                                                </FormControl>
                                            </Stack>
                                        </Paper>

                                        <Stack direction={{xs: 'column', sm: 'row'}} spacing={1.5}>
                                            <Button
                                                onClick={handleSubmit}
                                                variant="contained"
                                                color="secondary"
                                                startIcon={alreadyTracked ? <SaveOutlinedIcon/> : <PlaylistAddIcon/>}
                                                disabled={saving || removing}
                                                fullWidth
                                            >
                                                {saving ? (alreadyTracked ? 'Saving changes' : 'Adding to list') : (alreadyTracked ? 'Save changes' : 'Add to my list')}
                                            </Button>
                                            {alreadyTracked && (
                                                <Button
                                                    onClick={handleDeleteClick}
                                                    variant="outlined"
                                                    color="error"
                                                    startIcon={<DeleteIcon/>}
                                                    disabled={saving || removing}
                                                    fullWidth
                                                >
                                                    {removing ? 'Removing entry' : 'Remove from list'}
                                                </Button>
                                            )}
                                        </Stack>
                                    </Stack>
                                </Paper>

                                <Paper sx={{p: 3, borderRadius: '1.5rem', flex: 0.9}}>
                                    <Stack spacing={2}>
                                        <Typography variant="h6" fontWeight={800}>At a glance</Typography>
                                        <Divider/>
                                        <Box>
                                            <Typography variant="body2" color="text.secondary">Publisher</Typography>
                                            <Typography fontWeight={700}>{game.provider || 'Unknown publisher'}</Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="body2" color="text.secondary">Platforms</Typography>
                                            <Typography fontWeight={700}>{game.platforms || 'Platform unknown'}</Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="body2" color="text.secondary">Release date</Typography>
                                            <Typography fontWeight={700}>{formatDate(game.releaseDate || game.created)}</Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="body2" color="text.secondary">Categories</Typography>
                                            <Typography fontWeight={700}>{game.categories || 'No categories synced yet'}</Typography>
                                        </Box>
                                        <Box>
                                            <Typography variant="body2" color="text.secondary">Current list state</Typography>
                                            <Typography fontWeight={700}>
                                                {alreadyTracked
                                                    ? `${wishlistEntry?.status || status} • ${ratingSummary}${selectedPlatform ? ` • ${selectedPlatform}` : ''}`
                                                    : 'Not yet in your list'}
                                            </Typography>
                                        </Box>
                                    </Stack>
                                </Paper>
                            </Stack>
                        </Box>
                    </>
                ) : (
                    <Box sx={{p: 4}}>
                        <Typography variant="h6" fontWeight={800}>Could not load this game entry.</Typography>
                    </Box>
                )}
            </Paper>
        </Modal>
    );
};

export default ListModificationModal;
