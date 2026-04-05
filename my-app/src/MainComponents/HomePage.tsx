import React, {useCallback, useEffect, useState} from 'react';
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
import SyncIcon from '@mui/icons-material/Sync';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {CatalogDashboard, CatalogSyncRun} from "../GeneralComponents/types/CatalogDashboard";
import {GameData} from "../GeneralComponents/types/GameData";
import {useLoading} from "../GeneralComponents/LoadingContext";
import ListModificationModal from "./ListModificationModal";
import GameShowcaseCard from "./GameShowcaseCard";
import {GameStatusTypes} from "../GeneralComponents/types/GameStatusTypes";
import {formatDisplayDate} from "../GeneralComponents/utils/dates";
import {getErrorMessage} from "../api/client";
import {getCatalogDashboard, getLatestCatalogSync, removeWishlistEntry, triggerCatalogSync, upsertWishlist} from "../api/games";
import {requiresPlatformSelection} from "../GeneralComponents/utils/platforms";

function HomePage() {
    const [dashboard, setDashboard] = useState<CatalogDashboard | null>(null);
    const [message, setMessage] = useState('');
    const [severity, setSeverity] = useState(Severity.SUCCESS);
    const [modalGameId, setModalGameId] = useState<string | null>(null);
    const [pollSyncStatus, setPollSyncStatus] = useState(false);

    const loadingContext = useLoading();

    if (!loadingContext) {
        throw new Error("useLoading must be used within a LoadingProvider");
    }

    const {loading, setLoading} = loadingContext;

    const loadDashboard = useCallback(async () => {
        try {
            setLoading(true);
            const nextDashboard = await getCatalogDashboard();
            setDashboard(nextDashboard);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setLoading(false);
        }
    }, [setLoading]);

    useEffect(() => {
        loadDashboard();
    }, [loadDashboard]);

    useEffect(() => {
        if (!pollSyncStatus && !dashboard?.syncInProgress) {
            return;
        }

        const intervalId = window.setInterval(async () => {
            try {
                const latestRun = await getLatestCatalogSync();
                setDashboard(previous => previous ? {
                    ...previous,
                    syncInProgress: latestRun?.status === 'RUNNING',
                    lastSync: latestRun
                } : previous);

                if (latestRun?.status && latestRun.status !== 'RUNNING') {
                    setPollSyncStatus(false);
                    await loadDashboard();
                }
            } catch (error) {
                setPollSyncStatus(false);
                setMessage(getErrorMessage(error));
                setSeverity(Severity.ERROR);
            }
        }, 4000);

        return () => window.clearInterval(intervalId);
    }, [pollSyncStatus, dashboard?.syncInProgress, loadDashboard]);

    async function handleTriggerSync() {
        try {
            const syncRun = await triggerCatalogSync();
            setDashboard(previous => previous ? {
                ...previous,
                syncInProgress: true,
                lastSync: syncRun
            } : previous);
            setPollSyncStatus(true);
            setMessage('Catalog sync started. New games will flow into Postgres as batches finish.');
            setSeverity(Severity.INFO);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        }
    }

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
        setDashboard(previous => {
            if (!previous) {
                return previous;
            }

            return {
                ...previous,
                latestGames: previous.latestGames.map(game => game.id === gameId ? {...game, inWishlist} : game),
                popularGames: previous.popularGames.map(game => game.id === gameId ? {...game, inWishlist} : game)
            };
        });
    }

    function openWishlistModal(gameId: string) {
        setModalGameId(gameId);
    }

    function closeWishlistModal() {
        setModalGameId(null);
    }

    function formatSyncSummary(syncRun: CatalogSyncRun | null | undefined) {
        if (!syncRun) {
            return "No sync has been recorded yet.";
        }

        const startedAt = syncRun.startedAt ? new Date(syncRun.startedAt).toLocaleString() : "unknown";
        return `${syncRun.catalogSource} • ${syncRun.status} • ${syncRun.processedCount} processed • ${startedAt}`;
    }

    function formatDate(dateValue?: string) {
        return formatDisplayDate(dateValue, undefined, "Unknown");
    }

    const stats = dashboard ? [
        {label: 'Games in catalog', value: dashboard.totalGames.toLocaleString()},
        {label: 'Users', value: dashboard.totalUsers.toLocaleString()},
        {label: 'Wishlist entries', value: dashboard.totalWishlistEntries.toLocaleString()}
    ] : [];

    return (
        <Box sx={{width: '100%', pt: {xs: 10, md: 12}, pb: 6}}>
            <PopupMessage message={message} severity={severity}/>
            <Container maxWidth="xl">
                <Paper
                    sx={{
                        p: {xs: 3, md: 4},
                        borderRadius: '2rem',
                        mb: 4,
                        color: 'white',
                        background: 'linear-gradient(135deg, rgba(37,14,74,0.92), rgba(124,66,196,0.86))',
                        boxShadow: '0 30px 80px rgba(26, 7, 57, 0.34)'
                    }}
                >
                    <Stack direction={{xs: 'column', lg: 'row'}} spacing={3} justifyContent="space-between">
                        <Box maxWidth={760}>
                            <Typography variant="overline" sx={{letterSpacing: '0.16em', opacity: 0.85}}>
                                Catalog Control Room
                            </Typography>
                            <Typography variant="h3" fontWeight={900} sx={{mb: 1}}>
                                Keep the database alive, current, and actually useful.
                            </Typography>
                            <Typography sx={{opacity: 0.88, maxWidth: 640}}>
                                The backend now tracks sync runs, supports manual PSN imports, and bootstraps the catalog on an empty database.
                                This page shows catalog health, the latest imported games, and the titles people interact with most.
                            </Typography>
                        </Box>

                        <Paper
                            sx={{
                                p: 3,
                                minWidth: {xs: '100%', lg: 360},
                                borderRadius: '1.5rem',
                                background: 'rgba(255,255,255,0.12)',
                                color: 'white'
                            }}
                        >
                            <Stack spacing={2}>
                                <Stack direction="row" justifyContent="space-between" alignItems="center">
                                    <Typography variant="h6" fontWeight={800}>PSN Sync</Typography>
                                    <Chip
                                        label={dashboard?.syncInProgress ? 'Running' : dashboard?.lastSync?.status || 'Idle'}
                                        color={dashboard?.syncInProgress ? 'warning' : 'success'}
                                        size="small"
                                    />
                                </Stack>
                                <Typography variant="body2" sx={{opacity: 0.9}}>
                                    {formatSyncSummary(dashboard?.lastSync)}
                                </Typography>
                                {dashboard?.lastSync?.message && (
                                    <Typography variant="body2" sx={{opacity: 0.75}}>
                                        {dashboard.lastSync.message}
                                    </Typography>
                                )}
                                <Button
                                    onClick={handleTriggerSync}
                                    disabled={dashboard?.syncInProgress}
                                    variant="contained"
                                    color="secondary"
                                    startIcon={dashboard?.syncInProgress ? <CircularProgress size={18} color="inherit"/> : <SyncIcon/>}
                                >
                                    {dashboard?.syncInProgress ? 'Syncing catalog' : 'Sync catalog now'}
                                </Button>
                            </Stack>
                        </Paper>
                    </Stack>
                </Paper>

                <Grid container spacing={3} sx={{mb: 4}}>
                    {stats.map(stat => (
                        <Grid item xs={12} md={4} key={stat.label}>
                            <Paper
                                sx={{
                                    p: 3,
                                    borderRadius: '1.5rem',
                                    background: 'rgba(255,255,255,0.82)',
                                    boxShadow: '0 16px 40px rgba(33, 11, 68, 0.14)'
                                }}
                            >
                                <Typography variant="body2" color="text.secondary">{stat.label}</Typography>
                                <Typography variant="h4" fontWeight={900}>{stat.value}</Typography>
                            </Paper>
                        </Grid>
                    ))}
                </Grid>

                {loading && !dashboard && (
                    <Box sx={{display: 'flex', justifyContent: 'center', py: 8}}>
                        <CircularProgress/>
                    </Box>
                )}

                {dashboard && (
                    <Stack spacing={5}>
                        <Box>
                            <Stack direction="row" alignItems="center" spacing={1} sx={{mb: 2}}>
                                <AutoAwesomeIcon color="secondary"/>
                                <Typography variant="h4" fontWeight={900}>Latest arrivals</Typography>
                            </Stack>
                            <Grid container spacing={3}>
                                {dashboard.latestGames.map(game => (
                                    <Grid item xs={12} md={6} lg={3} key={game.id}>
                                        <GameShowcaseCard
                                            game={game}
                                            badgeLabel={formatDate(game.created)}
                                            formatDate={formatDate}
                                            onCustomize={openWishlistModal}
                                            onQuickToggle={(selectedGame) => selectedGame.inWishlist ? removeFromWishlist(selectedGame) : quickPlanToPlay(selectedGame)}
                                        />
                                    </Grid>
                                ))}
                            </Grid>
                        </Box>

                        <Box>
                            <Stack direction="row" alignItems="center" spacing={1} sx={{mb: 2}}>
                                <SyncIcon color="secondary"/>
                                <Typography variant="h4" fontWeight={900}>Most active titles</Typography>
                            </Stack>
                            <Grid container spacing={3}>
                                {dashboard.popularGames.map(game => (
                                    <Grid item xs={12} md={6} lg={3} key={game.id}>
                                        <GameShowcaseCard
                                            game={game}
                                            badgeLabel={`${game.interactions || '0'} interactions`}
                                            formatDate={formatDate}
                                            onCustomize={openWishlistModal}
                                            onQuickToggle={(selectedGame) => selectedGame.inWishlist ? removeFromWishlist(selectedGame) : quickPlanToPlay(selectedGame)}
                                        />
                                    </Grid>
                                ))}
                            </Grid>
                        </Box>
                    </Stack>
                )}

                {modalGameId && (
                    <ListModificationModal
                        open={!!modalGameId}
                        onClose={closeWishlistModal}
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

export default HomePage;
