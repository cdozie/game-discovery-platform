import React, {useCallback, useEffect, useMemo, useRef, useState} from "react";
import axios from "axios";
import {
    Box,
    Button,
    Chip,
    CircularProgress,
    Container,
    Dialog,
    DialogActions,
    DialogContent,
    DialogTitle,
    MenuItem,
    Paper,
    Stack,
    Tab,
    Tabs,
    TextField,
    Typography
} from "@mui/material";
import {alpha, useTheme} from "@mui/material/styles";
import AutoAwesomeIcon from "@mui/icons-material/AutoAwesome";
import BlockIcon from "@mui/icons-material/Block";
import EditOutlinedIcon from "@mui/icons-material/EditOutlined";
import GavelOutlinedIcon from "@mui/icons-material/GavelOutlined";
import LibraryAddOutlinedIcon from "@mui/icons-material/LibraryAddOutlined";
import PlaylistAddCheckCircleOutlinedIcon from "@mui/icons-material/PlaylistAddCheckCircleOutlined";
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {AdminAuditLog} from "../GeneralComponents/types/AdminAuditLog";
import {AdminDashboard} from "../GeneralComponents/types/AdminDashboard";
import {AdminGame} from "../GeneralComponents/types/AdminGame";
import {AdminUser} from "../GeneralComponents/types/AdminUser";
import {GameSubmission} from "../GeneralComponents/types/GameSubmission";
import {SystemJob} from "../GeneralComponents/types/SystemJob";
import {formatTenPointRating} from "../GeneralComponents/utils/ratings";
import {getErrorMessage} from "../api/client";
import {
    banAdminUser,
    clearAdminSpotlightGame,
    createAdminGame,
    deleteAdminGame,
    getAdminDashboard,
    getAdminGames,
    getAdminJobs,
    getAdminSubmissions,
    getAdminAuditLogs,
    getAdminUsers,
    queueAdminJob,
    reviewAdminSubmission,
    setAdminSpotlightGame,
    unbanAdminUser,
    updateAdminSubmission,
    updateAdminGame
} from "../api/games";

type AdminTab = 'overview' | 'submissions' | 'users' | 'banned' | 'catalog' | 'jobs' | 'audit';
type GameEditorState = {
    id?: string;
    name: string;
    description: string;
    categories: string;
    platforms: string;
    providerName: string;
    imageUrl: string;
    releaseDate: string;
    url: string;
    price: string;
    syncProtectedFields: string[];
};

type SubmissionEditorState = {
    name: string;
    description: string;
    categories: string;
    platforms: string;
    providerName: string;
    imageUrl: string;
    releaseDate: string;
    url: string;
};

const EMPTY_GAME_EDITOR: GameEditorState = {
    name: '',
    description: '',
    categories: '',
    platforms: '',
    providerName: '',
    imageUrl: '',
    releaseDate: '',
    url: '',
    price: '',
    syncProtectedFields: []
};

const SUBMISSIONS_PAGE_SIZE = 10;

function formatDateTime(dateValue?: string | null) {
    if (!dateValue) {
        return 'Unknown';
    }

    const date = new Date(dateValue);
    if (Number.isNaN(date.getTime())) {
        return dateValue;
    }

    return date.toLocaleString();
}

function splitDelimitedValues(rawValue?: string | null) {
    return (rawValue || '')
        .split(',')
        .map(value => value.trim())
        .filter(Boolean);
}

function formatSubmissionStatus(status?: string | null) {
    return (status || 'PENDING')
        .toLowerCase()
        .replace(/_/g, ' ')
        .replace(/\b\w/g, character => character.toUpperCase());
}

function buildSubmissionEditor(submission: GameSubmission): SubmissionEditorState {
    return {
        name: submission.name || '',
        description: submission.description || '',
        categories: submission.categories || '',
        platforms: submission.platforms || '',
        providerName: submission.providerName || '',
        imageUrl: submission.imageUrl || '',
        releaseDate: submission.releaseDate || '',
        url: submission.url || ''
    };
}

function AdminPortalPage() {
    const theme = useTheme();
    const [activeTab, setActiveTab] = useState<AdminTab>('overview');
    const [dashboard, setDashboard] = useState<AdminDashboard | null>(null);
    const [submissions, setSubmissions] = useState<GameSubmission[]>([]);
    const [submissionsHasMore, setSubmissionsHasMore] = useState(false);
    const [submissionsLoadingInitial, setSubmissionsLoadingInitial] = useState(false);
    const [submissionsLoadingMore, setSubmissionsLoadingMore] = useState(false);
    const [submissionStatusFilter, setSubmissionStatusFilter] = useState('PENDING');
    const [submissionNotes, setSubmissionNotes] = useState<Record<string, string>>({});
    const [editingSubmissionId, setEditingSubmissionId] = useState<string | null>(null);
    const [submissionEditors, setSubmissionEditors] = useState<Record<string, SubmissionEditorState>>({});
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [bannedUsers, setBannedUsers] = useState<AdminUser[]>([]);
    const [auditLogs, setAuditLogs] = useState<AdminAuditLog[]>([]);
    const [jobs, setJobs] = useState<SystemJob[]>([]);
    const [userQuery, setUserQuery] = useState('');
    const [games, setGames] = useState<AdminGame[]>([]);
    const [gameQuery, setGameQuery] = useState('');
    const [editorOpen, setEditorOpen] = useState(false);
    const [gameEditor, setGameEditor] = useState<GameEditorState>(EMPTY_GAME_EDITOR);
    const [banDialogOpen, setBanDialogOpen] = useState(false);
    const [banTargetUser, setBanTargetUser] = useState<AdminUser | null>(null);
    const [banReason, setBanReason] = useState('');
    const [banDuration, setBanDuration] = useState('7');
    const [customBanDays, setCustomBanDays] = useState('14');
    const [busyKey, setBusyKey] = useState<string | null>(null);
    const [message, setMessage] = useState('');
    const [severity, setSeverity] = useState<Severity>(Severity.SUCCESS);
    const [accessDenied, setAccessDenied] = useState(false);
    const submissionsSentinelRef = useRef<HTMLDivElement | null>(null);
    const submissionsRequestIdRef = useRef(0);

    const loadDashboard = useCallback(async () => {
        setDashboard(await getAdminDashboard());
    }, []);

    const loadSubmissions = useCallback(async (offset = 0, append = false) => {
        const nextRequestId = submissionsRequestIdRef.current + 1;
        submissionsRequestIdRef.current = nextRequestId;
        if (append) {
            setSubmissionsLoadingMore(true);
        } else {
            setSubmissionsLoadingInitial(true);
        }

        try {
            const response = await getAdminSubmissions(
                submissionStatusFilter === 'ALL' ? undefined : submissionStatusFilter,
                SUBMISSIONS_PAGE_SIZE,
                offset
            );
            if (submissionsRequestIdRef.current !== nextRequestId) {
                return;
            }

            setSubmissions(previous => append ? [...previous, ...response.items] : response.items);
            setSubmissionsHasMore(response.hasMore);
        } finally {
            if (submissionsRequestIdRef.current === nextRequestId) {
                setSubmissionsLoadingInitial(false);
                setSubmissionsLoadingMore(false);
            }
        }
    }, [submissionStatusFilter]);

    const loadUsers = useCallback(async () => {
        setUsers(await getAdminUsers(userQuery || undefined, false));
        setBannedUsers(await getAdminUsers('', true));
    }, [userQuery]);

    const loadGames = useCallback(async () => {
        setGames(await getAdminGames(gameQuery || undefined, 30));
    }, [gameQuery]);

    const loadAuditLogs = useCallback(async () => {
        setAuditLogs(await getAdminAuditLogs(80));
    }, []);

    const loadJobs = useCallback(async () => {
        setJobs(await getAdminJobs(40));
    }, []);

    const reloadAll = useCallback(async () => {
        try {
            setAccessDenied(false);
            await Promise.all([loadDashboard(), loadUsers(), loadGames(), loadAuditLogs(), loadJobs()]);
        } catch (error) {
            if (axios.isAxiosError(error) && error.response?.status === 403) {
                setAccessDenied(true);
                setMessage('Your account is signed in, but it does not have admin access.');
                setSeverity(Severity.ERROR);
                return;
            }
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        }
    }, [loadAuditLogs, loadDashboard, loadGames, loadJobs, loadUsers]);

    useEffect(() => {
        reloadAll().catch(() => null);
    }, [reloadAll]);

    useEffect(() => {
        loadSubmissions().catch(error => {
            if (axios.isAxiosError(error) && error.response?.status === 403) {
                setAccessDenied(true);
                setMessage('Your account is signed in, but it does not have admin access.');
                setSeverity(Severity.ERROR);
                return;
            }
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        });
    }, [loadSubmissions]);

    useEffect(() => {
        if (activeTab !== 'submissions' || !submissionsSentinelRef.current || !submissionsHasMore || submissionsLoadingInitial || submissionsLoadingMore) {
            return;
        }

        const observer = new IntersectionObserver(entries => {
            if (!entries[0]?.isIntersecting) {
                return;
            }

            loadSubmissions(submissions.length, true).catch(error => {
                setMessage(getErrorMessage(error));
                setSeverity(Severity.ERROR);
            });
        }, {rootMargin: '280px 0px'});

        observer.observe(submissionsSentinelRef.current);
        return () => observer.disconnect();
    }, [activeTab, loadSubmissions, submissions.length, submissionsHasMore, submissionsLoadingInitial, submissionsLoadingMore]);

    const filteredUsers = useMemo(() => {
        const normalizedQuery = userQuery.trim().toLowerCase();
        if (!normalizedQuery) {
            return users;
        }

        return users.filter(user => [
            user.username,
            user.email,
            user.displayName,
            user.favoritePlatform,
            user.banReason
        ].filter(Boolean).join(' ').toLowerCase().includes(normalizedQuery));
    }, [userQuery, users]);

    function openCreateGameDialog() {
        setGameEditor(EMPTY_GAME_EDITOR);
        setEditorOpen(true);
    }

    function openEditGameDialog(game: AdminGame) {
        setGameEditor({
            id: game.id,
            name: game.name || '',
            description: game.description || '',
            categories: game.categories || '',
            platforms: game.platforms || '',
            providerName: game.providerName || '',
            imageUrl: game.imageUrl || '',
            releaseDate: game.releaseDate || '',
            url: game.url || '',
            price: game.price || '',
            syncProtectedFields: game.syncProtectedFields || []
        });
        setEditorOpen(true);
    }

    async function handleSubmissionReview(submissionId: string, status: 'APPROVED' | 'REJECTED') {
        try {
            setBusyKey(`submission:${submissionId}:${status}`);
            if (editingSubmissionId === submissionId && submissionEditors[submissionId]) {
                await updateAdminSubmission(submissionId, submissionEditors[submissionId]);
            }
            await reviewAdminSubmission(submissionId, {
                status,
                reviewNotes: submissionNotes[submissionId] || null
            });
            setEditingSubmissionId(current => current === submissionId ? null : current);
            setMessage(status === 'APPROVED' ? 'Submission approved.' : 'Submission rejected.');
            setSeverity(Severity.SUCCESS);
            await Promise.all([loadDashboard(), loadSubmissions(), loadGames(), loadAuditLogs()]);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setBusyKey(null);
        }
    }

    function beginEditingSubmission(submission: GameSubmission) {
        setEditingSubmissionId(submission.id);
        setSubmissionEditors(previous => ({
            ...previous,
            [submission.id]: previous[submission.id] ?? buildSubmissionEditor(submission)
        }));
    }

    function cancelEditingSubmission(submissionId: string) {
        setEditingSubmissionId(current => current === submissionId ? null : current);
        setSubmissionEditors(previous => {
            const next = {...previous};
            delete next[submissionId];
            return next;
        });
    }

    function updateSubmissionEditorField(submissionId: string,
                                         field: keyof SubmissionEditorState,
                                         value: string) {
        const submission = submissions.find(entry => entry.id === submissionId);
        if (!submission) {
            return;
        }

        setSubmissionEditors(previous => ({
            ...previous,
            [submissionId]: {
                ...(previous[submissionId] ?? buildSubmissionEditor(submission)),
                [field]: value
            }
        }));
    }

    async function handleSaveSubmissionEdits(submissionId: string) {
        const editor = submissionEditors[submissionId];
        if (!editor) {
            return;
        }

        try {
            setBusyKey(`submission:${submissionId}:save`);
            await updateAdminSubmission(submissionId, editor);
            setEditingSubmissionId(current => current === submissionId ? null : current);
            setMessage('Submission edits saved.');
            setSeverity(Severity.SUCCESS);
            await Promise.all([loadSubmissions(), loadAuditLogs()]);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setBusyKey(null);
        }
    }

    async function handleBanUser() {
        if (!banTargetUser) {
            return;
        }

        try {
            setBusyKey(`ban:${banTargetUser.id}`);
            const permanent = banDuration === 'permanent';
            const durationDays = permanent ? null : Number(banDuration === 'custom' ? customBanDays : banDuration);
            await banAdminUser(banTargetUser.id, {
                permanent,
                durationDays,
                reason: banReason || null
            });
            setBanDialogOpen(false);
            setBanTargetUser(null);
            setBanReason('');
            setBanDuration('7');
            setCustomBanDays('14');
            setMessage('User banned.');
            setSeverity(Severity.SUCCESS);
            await Promise.all([loadDashboard(), loadUsers(), loadAuditLogs()]);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setBusyKey(null);
        }
    }

    async function handleUnbanUser(userId: string) {
        try {
            setBusyKey(`unban:${userId}`);
            await unbanAdminUser(userId);
            setMessage('User unbanned.');
            setSeverity(Severity.SUCCESS);
            await Promise.all([loadDashboard(), loadUsers(), loadAuditLogs()]);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setBusyKey(null);
        }
    }

    async function handleSaveGame() {
        try {
            setBusyKey(gameEditor.id ? `save:${gameEditor.id}` : 'create:game');
            if (gameEditor.id) {
                await updateAdminGame(gameEditor.id, gameEditor);
                setMessage('Game updated. Changed fields are now protected from sync.');
            } else {
                await createAdminGame(gameEditor);
                setMessage('Game added to the catalog.');
            }
            setSeverity(Severity.SUCCESS);
            setEditorOpen(false);
            await Promise.all([loadDashboard(), loadGames(), loadAuditLogs()]);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setBusyKey(null);
        }
    }

    async function handleDeleteGame(gameId: string) {
        if (!window.confirm('Delete this game from the catalog? This also removes dependent list entries and comments.')) {
            return;
        }

        try {
            setBusyKey(`delete:${gameId}`);
            await deleteAdminGame(gameId);
            setMessage('Game deleted.');
            setSeverity(Severity.SUCCESS);
            await Promise.all([loadDashboard(), loadGames(), loadAuditLogs()]);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setBusyKey(null);
        }
    }

    async function handleSetSpotlight(game: AdminGame) {
        try {
            setBusyKey(`spotlight:${game.id}`);
            setDashboard(await setAdminSpotlightGame(game.id));
            setMessage(`${game.name} is now the discovery spotlight.`);
            setSeverity(Severity.SUCCESS);
            await loadAuditLogs();
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setBusyKey(null);
        }
    }

    async function handleClearSpotlight() {
        try {
            setBusyKey('spotlight:clear');
            setDashboard(await clearAdminSpotlightGame());
            setMessage('Discovery spotlight cleared.');
            setSeverity(Severity.SUCCESS);
            await loadAuditLogs();
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setBusyKey(null);
        }
    }

    async function handleQueueJob(jobType: 'CATALOG_SYNC' | 'CATALOG_CONSOLIDATION' | 'GAME_STATS_REBUILD',
                                  catalogSource?: 'PLAYSTATION_STORE' | 'IGDB_PC_XBOX') {
        const busyId = catalogSource ? `job:${jobType}:${catalogSource}` : `job:${jobType}`;
        try {
            setBusyKey(busyId);
            const queuedJob = await queueAdminJob({
                jobType,
                catalogSource
            });
            setActiveTab('jobs');
            setMessage(`${queuedJob.type.replace(/_/g, ' ')} queued.`);
            setSeverity(Severity.SUCCESS);
            await Promise.all([loadJobs(), loadDashboard()]);
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setBusyKey(null);
        }
    }

    if (accessDenied) {
        return (
            <Box sx={{width: '100%', pt: {xs: 10, md: 12}, pb: 6}}>
                <Container maxWidth="md">
                    <PopupMessage message={message} severity={severity}/>
                    <Paper sx={{p: {xs: 3, md: 4}, borderRadius: '2rem', textAlign: 'center'}}>
                        <Stack spacing={2}>
                            <Typography variant="h3" fontWeight={900}>Admin Portal</Typography>
                            <Typography color="text.secondary">
                                This account is authenticated, but the backend did not grant it admin access.
                            </Typography>
                            <Typography color="text.secondary">
                                If this should be an admin account, log out and back in once. The server still treats the backend role as the source of truth.
                            </Typography>
                        </Stack>
                    </Paper>
                </Container>
            </Box>
        );
    }

    return (
        <Box sx={{width: '100%', pt: {xs: 10, md: 12}, pb: 6}}>
            <Container maxWidth="xl">
                <PopupMessage message={message} severity={severity}/>
                <Stack spacing={3}>
                    <Box>
                        <Typography variant="h3" fontWeight={900}>Admin Portal</Typography>
                        <Typography color="text.secondary" sx={{mt: 1}}>
                            Moderate submissions, manage bans, and keep catalog entries clean without leaving the app.
                        </Typography>
                    </Box>

                    <Paper sx={{borderRadius: '1.5rem', overflow: 'hidden'}}>
                        <Tabs
                            value={activeTab}
                            onChange={(_event, value) => setActiveTab(value)}
                            variant="scrollable"
                            scrollButtons="auto"
                        >
                            <Tab value="overview" label="Overview"/>
                            <Tab value="submissions" label="Submissions"/>
                            <Tab value="users" label="Users"/>
                            <Tab value="banned" label="Banned Users"/>
                            <Tab value="catalog" label="Catalog"/>
                            <Tab value="jobs" label="Jobs"/>
                            <Tab value="audit" label="Audit Log"/>
                        </Tabs>
                    </Paper>

                    {activeTab === 'overview' && (
                        <Stack spacing={2}>
                            <Box
                                sx={{
                                    display: 'grid',
                                    gap: 2,
                                    gridTemplateColumns: {xs: '1fr', md: 'repeat(3, minmax(0, 1fr))'}
                                }}
                            >
                                {[
                                    {label: 'Pending submissions', value: dashboard?.pendingSubmissions ?? 0},
                                    {label: 'Active bans', value: dashboard?.activeBans ?? 0},
                                    {label: 'Catalog games', value: dashboard?.totalGames ?? 0},
                                    {label: 'Users', value: dashboard?.totalUsers ?? 0},
                                    {label: 'Approved submissions', value: dashboard?.approvedSubmissions ?? 0},
                                    {label: 'Rejected submissions', value: dashboard?.rejectedSubmissions ?? 0}
                                ].map(card => (
                                    <Paper key={card.label} sx={{p: 3, borderRadius: '1.5rem'}}>
                                        <Typography color="text.secondary">{card.label}</Typography>
                                        <Typography variant="h3" fontWeight={900} sx={{mt: 1}}>{card.value}</Typography>
                                    </Paper>
                                ))}
                            </Box>

                            <Paper
                                sx={{
                                    p: 3,
                                    borderRadius: '1.75rem',
                                    background: `linear-gradient(135deg, ${alpha(theme.palette.primary.main, 0.12)} 0%, ${alpha(theme.palette.secondary.main, 0.12)} 100%)`
                                }}
                            >
                                <Stack direction={{xs: 'column', lg: 'row'}} justifyContent="space-between" spacing={2}>
                                    <Box>
                                        <Stack direction="row" spacing={1} alignItems="center">
                                            <AutoAwesomeIcon color="secondary"/>
                                            <Typography variant="h5" fontWeight={900}>Discovery Spotlight</Typography>
                                        </Stack>
                                        <Typography color="text.secondary" sx={{mt: 1}}>
                                            {dashboard?.spotlightGameName
                                                ? `${dashboard.spotlightGameName} is currently pinned as the discovery hero.`
                                                : 'No manual spotlight is pinned right now. Discovery falls back to the automatic pick.'}
                                        </Typography>
                                    </Box>
                                    <Stack direction={{xs: 'column', sm: 'row'}} spacing={1.5}>
                                        {dashboard?.spotlightGameName && (
                                            <Chip label={dashboard.spotlightGameName} color="secondary" sx={{fontWeight: 800}}/>
                                        )}
                                        <Button
                                            variant="outlined"
                                            color="secondary"
                                            disabled={!dashboard?.spotlightGameId || busyKey === 'spotlight:clear'}
                                            onClick={handleClearSpotlight}
                                        >
                                            {busyKey === 'spotlight:clear' ? 'Clearing' : 'Clear spotlight'}
                                        </Button>
                                    </Stack>
                                </Stack>
                            </Paper>

                            <Paper sx={{p: 3, borderRadius: '1.75rem'}}>
                                <Stack spacing={2}>
                                    <Box>
                                        <Typography variant="h5" fontWeight={900}>Maintenance jobs</Typography>
                                        <Typography color="text.secondary" sx={{mt: 0.75}}>
                                            Queue syncs and heavy maintenance work instead of tying them to a live admin request.
                                        </Typography>
                                    </Box>
                                    <Stack direction={{xs: 'column', md: 'row'}} spacing={1.5} flexWrap="wrap">
                                        <Button
                                            variant="contained"
                                            onClick={() => handleQueueJob('CATALOG_SYNC', 'PLAYSTATION_STORE')}
                                            disabled={busyKey === 'job:CATALOG_SYNC:PLAYSTATION_STORE'}
                                        >
                                            {busyKey === 'job:CATALOG_SYNC:PLAYSTATION_STORE' ? 'Queueing PSN sync' : 'Queue PSN sync'}
                                        </Button>
                                        <Button
                                            variant="contained"
                                            color="secondary"
                                            onClick={() => handleQueueJob('CATALOG_SYNC', 'IGDB_PC_XBOX')}
                                            disabled={busyKey === 'job:CATALOG_SYNC:IGDB_PC_XBOX'}
                                        >
                                            {busyKey === 'job:CATALOG_SYNC:IGDB_PC_XBOX' ? 'Queueing PC/Xbox sync' : 'Queue PC/Xbox sync'}
                                        </Button>
                                        <Button
                                            variant="outlined"
                                            onClick={() => handleQueueJob('CATALOG_CONSOLIDATION')}
                                            disabled={busyKey === 'job:CATALOG_CONSOLIDATION'}
                                        >
                                            {busyKey === 'job:CATALOG_CONSOLIDATION' ? 'Queueing consolidation' : 'Queue consolidation'}
                                        </Button>
                                        <Button
                                            variant="outlined"
                                            color="secondary"
                                            onClick={() => handleQueueJob('GAME_STATS_REBUILD')}
                                            disabled={busyKey === 'job:GAME_STATS_REBUILD'}
                                        >
                                            {busyKey === 'job:GAME_STATS_REBUILD' ? 'Queueing stats rebuild' : 'Queue stats rebuild'}
                                        </Button>
                                    </Stack>
                                </Stack>
                            </Paper>
                        </Stack>
                    )}

                    {activeTab === 'submissions' && (
                        <Paper sx={{p: 3, borderRadius: '1.5rem'}}>
                            <Stack spacing={2.5}>
                                <Stack direction={{xs: 'column', md: 'row'}} spacing={2} justifyContent="space-between">
                                    <Typography variant="h4" fontWeight={900}>Game submissions</Typography>
                                    <TextField
                                        select
                                        label="Status"
                                        value={submissionStatusFilter}
                                        onChange={(event) => setSubmissionStatusFilter(event.target.value)}
                                        sx={{minWidth: 220}}
                                    >
                                        <MenuItem value="PENDING">Pending</MenuItem>
                                        <MenuItem value="APPROVED">Approved</MenuItem>
                                        <MenuItem value="REJECTED">Rejected</MenuItem>
                                        <MenuItem value="ALL">All</MenuItem>
                                    </TextField>
                                </Stack>

                                {submissionsLoadingInitial && submissions.length === 0 ? (
                                    <Box sx={{display: 'flex', justifyContent: 'center', py: 5}}>
                                        <CircularProgress size={28}/>
                                    </Box>
                                ) : submissions.length === 0 ? (
                                    <Typography color="text.secondary">No submissions matched that filter.</Typography>
                                ) : submissions.map(submission => {
                                    const statusColor = submission.status === 'APPROVED'
                                        ? theme.palette.success.main
                                        : submission.status === 'REJECTED'
                                            ? theme.palette.error.main
                                            : theme.palette.warning.main;
                                    const isEditingSubmission = editingSubmissionId === submission.id;
                                    const submissionEditor = submissionEditors[submission.id] ?? buildSubmissionEditor(submission);
                                    const canAdminEditSubmission = submission.status !== 'APPROVED' && submission.status !== 'WITHDRAWN';
                                    const canReviewSubmission = submission.status === 'PENDING';
                                    const displayedName = isEditingSubmission ? submissionEditor.name : submission.name;
                                    const displayedDescription = isEditingSubmission ? submissionEditor.description : (submission.description || '');
                                    const displayedPlatforms = isEditingSubmission ? submissionEditor.platforms : (submission.platforms || '');
                                    const displayedCategories = isEditingSubmission ? submissionEditor.categories : (submission.categories || '');
                                    const displayedProviderName = isEditingSubmission ? submissionEditor.providerName : (submission.providerName || '');
                                    const displayedReleaseDate = isEditingSubmission ? submissionEditor.releaseDate : (submission.releaseDate || '');
                                    const displayedImageUrl = isEditingSubmission ? submissionEditor.imageUrl : (submission.imageUrl || '');
                                    const displayedUrl = isEditingSubmission ? submissionEditor.url : (submission.url || '');
                                    const platformLabels = splitDelimitedValues(displayedPlatforms);
                                    const categoryLabels = splitDelimitedValues(displayedCategories);

                                    return (
                                        <Paper
                                            key={submission.id}
                                            variant="outlined"
                                            sx={{
                                                borderRadius: '1.5rem',
                                                overflow: 'hidden',
                                                borderColor: alpha(statusColor, 0.28),
                                                background: `linear-gradient(180deg, ${alpha(theme.palette.background.paper, 0.98)} 0%, ${alpha(statusColor, 0.06)} 100%)`,
                                                boxShadow: `0 18px 40px ${alpha(theme.palette.common.black, 0.06)}`
                                            }}
                                        >
                                            <Box
                                                sx={{
                                                    px: {xs: 2.25, md: 2.75},
                                                    py: {xs: 2, md: 2.5},
                                                    borderBottom: `1px solid ${alpha(statusColor, 0.16)}`,
                                                    background: `linear-gradient(135deg, ${alpha(statusColor, 0.14)} 0%, ${alpha(theme.palette.primary.light, 0.06)} 100%)`
                                                }}
                                            >
                                                <Stack direction={{xs: 'column', lg: 'row'}} justifyContent="space-between" spacing={1.5}>
                                                    <Box sx={{minWidth: 0}}>
                                                        <Typography
                                                            variant="overline"
                                                            sx={{
                                                                display: 'block',
                                                                letterSpacing: '0.16em',
                                                                fontWeight: 900,
                                                                color: alpha(theme.palette.text.primary, 0.68)
                                                            }}
                                                        >
                                                            Submission #{submission.id}
                                                        </Typography>
                                                        <Typography variant="h5" fontWeight={900} sx={{mt: 0.5}}>
                                                            {displayedName}
                                                        </Typography>
                                                        <Typography color="text.secondary" sx={{mt: 0.5}}>
                                                            Proposed catalog entry from {submission.requestedByDisplayName || submission.requestedByUsername || 'unknown'}
                                                        </Typography>
                                                    </Box>
                                                    <Chip
                                                        label={formatSubmissionStatus(submission.status)}
                                                        sx={{
                                                            alignSelf: {xs: 'flex-start', lg: 'center'},
                                                            fontWeight: 800,
                                                            color: theme.palette.common.white,
                                                            backgroundColor: statusColor
                                                        }}
                                                    />
                                                </Stack>
                                            </Box>

                                            <Box
                                                sx={{
                                                    p: {xs: 2.25, md: 2.75},
                                                    display: 'grid',
                                                    gap: 2.5,
                                                    gridTemplateColumns: {xs: '1fr', xl: 'minmax(0, 1.7fr) minmax(280px, 0.9fr)'}
                                                }}
                                            >
                                                <Stack spacing={2.25}>
                                                    <Box
                                                        sx={{
                                                            p: 1.85,
                                                            borderRadius: '1.1rem',
                                                            border: `1px solid ${alpha(theme.palette.primary.main, 0.12)}`,
                                                            backgroundColor: alpha(theme.palette.primary.light, 0.05)
                                                        }}
                                                    >
                                                        <Typography variant="overline" sx={{display: 'block', fontWeight: 900, letterSpacing: '0.12em', color: 'text.secondary'}}>
                                                            Submitted title
                                                        </Typography>
                                                        {isEditingSubmission ? (
                                                            <TextField
                                                                fullWidth
                                                                label="Game title"
                                                                value={submissionEditor.name}
                                                                onChange={(event) => updateSubmissionEditorField(submission.id, 'name', event.target.value)}
                                                                sx={{
                                                                    mt: 1.1,
                                                                    '& .MuiOutlinedInput-root': {
                                                                        borderRadius: '1rem',
                                                                        backgroundColor: alpha(theme.palette.background.paper, 0.92)
                                                                    }
                                                                }}
                                                            />
                                                        ) : (
                                                            <Typography sx={{mt: 1.1, fontWeight: 800}}>
                                                                {submission.name}
                                                            </Typography>
                                                        )}
                                                    </Box>

                                                    <Box
                                                        sx={{
                                                            display: 'grid',
                                                            gap: 1.25,
                                                            gridTemplateColumns: {xs: '1fr', md: 'repeat(2, minmax(0, 1fr))'}
                                                        }}
                                                    >
                                                        {[
                                                            {
                                                                label: 'Submitted by',
                                                                value: submission.requestedByDisplayName || submission.requestedByUsername || 'Unknown reviewer',
                                                                editable: false
                                                            },
                                                            {
                                                                label: 'Submitted at',
                                                                value: formatDateTime(submission.createdAt),
                                                                editable: false
                                                            },
                                                            {
                                                                label: 'Release date',
                                                                value: displayedReleaseDate || 'Not supplied',
                                                                editable: true,
                                                                field: 'releaseDate' as const
                                                            },
                                                            {
                                                                label: 'Provider / studio',
                                                                value: displayedProviderName || 'Not supplied',
                                                                editable: true,
                                                                field: 'providerName' as const
                                                            }
                                                        ].map(item => (
                                                            <Box
                                                                key={item.label}
                                                                sx={{
                                                                    p: 1.5,
                                                                    borderRadius: '1rem',
                                                                    border: `1px solid ${alpha(theme.palette.primary.main, 0.12)}`,
                                                                    backgroundColor: alpha(theme.palette.primary.light, 0.06)
                                                                }}
                                                            >
                                                                <Typography variant="caption" sx={{display: 'block', color: 'text.secondary', fontWeight: 800}}>
                                                                    {item.label}
                                                                </Typography>
                                                                {isEditingSubmission && item.editable && item.field ? (
                                                                    <TextField
                                                                        fullWidth
                                                                        value={submissionEditor[item.field]}
                                                                        onChange={(event) => updateSubmissionEditorField(submission.id, item.field, event.target.value)}
                                                                        sx={{
                                                                            mt: 0.9,
                                                                            '& .MuiOutlinedInput-root': {
                                                                                borderRadius: '0.9rem',
                                                                                backgroundColor: alpha(theme.palette.background.paper, 0.92)
                                                                            }
                                                                        }}
                                                                    />
                                                                ) : (
                                                                    <Typography sx={{mt: 0.45, fontWeight: 700}}>
                                                                        {item.value}
                                                                    </Typography>
                                                                )}
                                                            </Box>
                                                        ))}
                                                    </Box>

                                                    <Box
                                                        sx={{
                                                            p: 1.5,
                                                            borderRadius: '1rem',
                                                            border: `1px solid ${alpha(theme.palette.warning.main, 0.18)}`,
                                                            background: `linear-gradient(180deg, ${alpha(theme.palette.background.paper, 0.92)}, ${alpha(theme.palette.warning.main, 0.05)})`
                                                        }}
                                                    >
                                                        <Typography variant="overline" sx={{display: 'block', fontWeight: 900, letterSpacing: '0.12em', color: 'text.secondary'}}>
                                                            Platforms
                                                        </Typography>
                                                        {isEditingSubmission ? (
                                                            <TextField
                                                                fullWidth
                                                                label="Platforms"
                                                                helperText="Comma-separated platforms"
                                                                value={submissionEditor.platforms}
                                                                onChange={(event) => updateSubmissionEditorField(submission.id, 'platforms', event.target.value)}
                                                                sx={{
                                                                    mt: 1,
                                                                    '& .MuiOutlinedInput-root': {
                                                                        borderRadius: '1rem',
                                                                        backgroundColor: alpha(theme.palette.background.paper, 0.92)
                                                                    }
                                                                }}
                                                            />
                                                        ) : platformLabels.length > 0 ? (
                                                            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{mt: 0.9}}>
                                                                {platformLabels.map(platform => (
                                                                    <Chip
                                                                        key={platform}
                                                                        label={platform}
                                                                        size="small"
                                                                        variant="filled"
                                                                        sx={{
                                                                            fontWeight: 800,
                                                                            color: '#8a4b00',
                                                                            border: `1px solid ${alpha(theme.palette.warning.main, 0.28)}`,
                                                                            backgroundColor: alpha(theme.palette.warning.main, 0.16),
                                                                            boxShadow: `inset 0 1px 0 ${alpha(theme.palette.common.white, 0.5)}`,
                                                                            '& .MuiChip-label': {
                                                                                color: '#8a4b00'
                                                                            }
                                                                        }}
                                                                    />
                                                                ))}
                                                            </Stack>
                                                        ) : (
                                                            <Typography sx={{mt: 1, color: 'text.secondary'}}>No platforms supplied.</Typography>
                                                        )}
                                                    </Box>

                                                    <Box
                                                        sx={{
                                                            p: 1.5,
                                                            borderRadius: '1rem',
                                                            border: `1px solid ${alpha(theme.palette.secondary.main, 0.18)}`,
                                                            background: `linear-gradient(180deg, ${alpha(theme.palette.background.paper, 0.92)}, rgba(124, 58, 237, 0.045))`
                                                        }}
                                                    >
                                                        <Typography variant="overline" sx={{display: 'block', fontWeight: 900, letterSpacing: '0.12em', color: 'text.secondary'}}>
                                                            Categories / genres
                                                        </Typography>
                                                        {isEditingSubmission ? (
                                                            <TextField
                                                                fullWidth
                                                                label="Categories / genres"
                                                                helperText="Comma-separated genres or categories"
                                                                value={submissionEditor.categories}
                                                                onChange={(event) => updateSubmissionEditorField(submission.id, 'categories', event.target.value)}
                                                                sx={{
                                                                    mt: 1,
                                                                    '& .MuiOutlinedInput-root': {
                                                                        borderRadius: '1rem',
                                                                        backgroundColor: alpha(theme.palette.background.paper, 0.92)
                                                                    }
                                                                }}
                                                            />
                                                        ) : categoryLabels.length > 0 ? (
                                                            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{mt: 0.9}}>
                                                                {categoryLabels.map(category => (
                                                                    <Chip
                                                                        key={category}
                                                                        label={category}
                                                                        size="small"
                                                                        variant="filled"
                                                                        sx={{
                                                                            fontWeight: 800,
                                                                            color: '#4c1d95',
                                                                            border: `1px solid ${alpha(theme.palette.secondary.main, 0.38)}`,
                                                                            backgroundColor: 'rgba(196, 181, 253, 0.9)',
                                                                            boxShadow: `0 6px 14px ${alpha(theme.palette.secondary.main, 0.12)}`,
                                                                            '& .MuiChip-label': {
                                                                                color: '#4c1d95',
                                                                                px: 0.4
                                                                            }
                                                                        }}
                                                                    />
                                                                ))}
                                                            </Stack>
                                                        ) : (
                                                            <Typography sx={{mt: 1, color: 'text.secondary'}}>No categories supplied.</Typography>
                                                        )}
                                                    </Box>

                                                    <Box
                                                        sx={{
                                                            p: 1.85,
                                                            borderRadius: '1.1rem',
                                                            border: `1px solid ${alpha(theme.palette.primary.main, 0.12)}`,
                                                            backgroundColor: alpha(theme.palette.primary.light, 0.05)
                                                        }}
                                                    >
                                                        <Typography variant="overline" sx={{display: 'block', fontWeight: 900, letterSpacing: '0.12em', color: 'text.secondary'}}>
                                                            About this submission
                                                        </Typography>
                                                        {isEditingSubmission ? (
                                                            <TextField
                                                                fullWidth
                                                                multiline
                                                                minRows={5}
                                                                label="Description"
                                                                value={submissionEditor.description}
                                                                onChange={(event) => updateSubmissionEditorField(submission.id, 'description', event.target.value)}
                                                                sx={{
                                                                    mt: 1.1,
                                                                    '& .MuiOutlinedInput-root': {
                                                                        borderRadius: '1rem',
                                                                        backgroundColor: alpha(theme.palette.background.paper, 0.92)
                                                                    }
                                                                }}
                                                            />
                                                        ) : (
                                                            <Typography sx={{mt: 1.1, color: displayedDescription ? 'text.primary' : 'text.secondary', lineHeight: 1.7}}>
                                                                {displayedDescription || 'No description was included with this request.'}
                                                            </Typography>
                                                        )}
                                                    </Box>

                                                    {(displayedUrl || isEditingSubmission) && (
                                                        <Box
                                                            sx={{
                                                                p: 1.6,
                                                                borderRadius: '1rem',
                                                                border: `1px solid ${alpha(theme.palette.info.main, 0.18)}`,
                                                                backgroundColor: alpha(theme.palette.info.light, 0.08)
                                                            }}
                                                        >
                                                            <Typography variant="caption" sx={{display: 'block', color: 'text.secondary', fontWeight: 800}}>
                                                                Reference link
                                                            </Typography>
                                                            {isEditingSubmission ? (
                                                                <TextField
                                                                    fullWidth
                                                                    label="Store / reference URL"
                                                                    value={submissionEditor.url}
                                                                    onChange={(event) => updateSubmissionEditorField(submission.id, 'url', event.target.value)}
                                                                    sx={{
                                                                        mt: 0.9,
                                                                        '& .MuiOutlinedInput-root': {
                                                                            borderRadius: '0.95rem',
                                                                            backgroundColor: alpha(theme.palette.background.paper, 0.92)
                                                                        }
                                                                    }}
                                                                />
                                                            ) : displayedUrl ? (
                                                                <Typography
                                                                    component="a"
                                                                    href={displayedUrl}
                                                                    target="_blank"
                                                                    rel="noreferrer"
                                                                    sx={{
                                                                        mt: 0.5,
                                                                        display: 'inline-block',
                                                                        fontWeight: 700,
                                                                        color: theme.palette.info.dark,
                                                                        textDecoration: 'none',
                                                                        wordBreak: 'break-all'
                                                                    }}
                                                                >
                                                                    {displayedUrl}
                                                                </Typography>
                                                            ) : (
                                                                <Typography sx={{mt: 0.5, color: 'text.secondary'}}>
                                                                    No reference link supplied.
                                                                </Typography>
                                                            )}
                                                        </Box>
                                                    )}

                                                    <Box
                                                        sx={{
                                                            p: 2,
                                                            borderRadius: '1.2rem',
                                                            border: `1px solid ${alpha(statusColor, 0.18)}`,
                                                            backgroundColor: alpha(statusColor, 0.07)
                                                        }}
                                                    >
                                                        <Typography variant="overline" sx={{display: 'block', fontWeight: 900, letterSpacing: '0.12em', color: 'text.secondary'}}>
                                                            Reviewer workspace
                                                        </Typography>
                                                        <Typography variant="body2" color="text.secondary" sx={{mt: 0.75}}>
                                                            {canReviewSubmission
                                                                ? 'Add a note that explains why this submission is being approved or rejected.'
                                                                : 'This submission is already finalized. Review actions are disabled.'}
                                                        </Typography>
                                                        <TextField
                                                            label="Admin review note"
                                                            value={submissionNotes[submission.id] ?? submission.reviewNotes ?? ''}
                                                            onChange={(event) => setSubmissionNotes(previous => ({...previous, [submission.id]: event.target.value}))}
                                                            multiline
                                                            minRows={3}
                                                            fullWidth
                                                            sx={{
                                                                mt: 1.5,
                                                                '& .MuiOutlinedInput-root': {
                                                                    borderRadius: '1rem',
                                                                    backgroundColor: alpha(theme.palette.background.paper, 0.95)
                                                                }
                                                            }}
                                                            disabled={!canReviewSubmission}
                                                        />
                                                        <Stack direction={{xs: 'column', sm: 'row'}} spacing={1.5} sx={{mt: 1.6}}>
                                                            {canAdminEditSubmission && !isEditingSubmission && (
                                                                <Button
                                                                    variant="outlined"
                                                                    startIcon={<EditOutlinedIcon/>}
                                                                    disabled={busyKey === `submission:${submission.id}:save`}
                                                                    onClick={() => beginEditingSubmission(submission)}
                                                                >
                                                                    Edit submission
                                                                </Button>
                                                            )}
                                                            {canAdminEditSubmission && isEditingSubmission && (
                                                                <Button
                                                                    variant="contained"
                                                                    color="secondary"
                                                                    startIcon={<EditOutlinedIcon/>}
                                                                    disabled={busyKey === `submission:${submission.id}:save`}
                                                                    onClick={() => handleSaveSubmissionEdits(submission.id)}
                                                                >
                                                                    {busyKey === `submission:${submission.id}:save` ? 'Saving edits' : 'Save edits'}
                                                                </Button>
                                                            )}
                                                            {canAdminEditSubmission && isEditingSubmission && (
                                                                <Button
                                                                    variant="text"
                                                                    onClick={() => cancelEditingSubmission(submission.id)}
                                                                >
                                                                    Cancel editing
                                                                </Button>
                                                            )}
                                                            {canReviewSubmission && (
                                                                <Button
                                                                    variant="contained"
                                                                    color="success"
                                                                    startIcon={<PlaylistAddCheckCircleOutlinedIcon/>}
                                                                    disabled={busyKey === `submission:${submission.id}:APPROVED`}
                                                                    onClick={() => handleSubmissionReview(submission.id, 'APPROVED')}
                                                                >
                                                                    {busyKey === `submission:${submission.id}:APPROVED` ? 'Approving' : 'Approve Submission'}
                                                                </Button>
                                                            )}
                                                            {canReviewSubmission && (
                                                                <Button
                                                                    variant="outlined"
                                                                    color="error"
                                                                    startIcon={<BlockIcon/>}
                                                                    disabled={busyKey === `submission:${submission.id}:REJECTED`}
                                                                    onClick={() => handleSubmissionReview(submission.id, 'REJECTED')}
                                                                >
                                                                    {busyKey === `submission:${submission.id}:REJECTED` ? 'Rejecting' : 'Reject Submission'}
                                                                </Button>
                                                            )}
                                                        </Stack>
                                                    </Box>
                                                </Stack>

                                                <Stack spacing={2}>
                                                    <Box
                                                        sx={{
                                                            p: 1.5,
                                                            borderRadius: '1.25rem',
                                                            border: `1px solid ${alpha(theme.palette.primary.main, 0.14)}`,
                                                            backgroundColor: alpha(theme.palette.common.white, 0.75)
                                                        }}
                                                    >
                                                        <Typography variant="overline" sx={{display: 'block', fontWeight: 900, letterSpacing: '0.12em', color: 'text.secondary'}}>
                                                            Submitted artwork
                                                        </Typography>
                                                        {isEditingSubmission && (
                                                            <TextField
                                                                fullWidth
                                                                label="Artwork URL or data URI"
                                                                value={submissionEditor.imageUrl}
                                                                onChange={(event) => updateSubmissionEditorField(submission.id, 'imageUrl', event.target.value)}
                                                                sx={{
                                                                    mt: 1,
                                                                    '& .MuiOutlinedInput-root': {
                                                                        borderRadius: '0.95rem',
                                                                        backgroundColor: alpha(theme.palette.background.paper, 0.92)
                                                                    }
                                                                }}
                                                            />
                                                        )}
                                                        {displayedImageUrl ? (
                                                            <Box
                                                                sx={{
                                                                    mt: 1.2,
                                                                    overflow: 'hidden',
                                                                    borderRadius: '1rem',
                                                                    border: `1px solid ${alpha(theme.palette.primary.main, 0.12)}`,
                                                                    backgroundColor: alpha(theme.palette.background.paper, 0.92)
                                                                }}
                                                            >
                                                                <Box
                                                                    component="img"
                                                                    src={displayedImageUrl}
                                                                    alt={`${displayedName} submitted cover`}
                                                                    sx={{
                                                                        display: 'block',
                                                                        width: '100%',
                                                                        maxHeight: 320,
                                                                        objectFit: 'cover'
                                                                    }}
                                                                />
                                                            </Box>
                                                        ) : (
                                                            <Box
                                                                sx={{
                                                                    mt: 1.2,
                                                                    p: 2.5,
                                                                    borderRadius: '1rem',
                                                                    border: `1px dashed ${alpha(theme.palette.text.primary, 0.18)}`,
                                                                    backgroundColor: alpha(theme.palette.background.default, 0.55)
                                                                }}
                                                            >
                                                                <Typography color="text.secondary">
                                                                    No artwork was attached to this submission.
                                                                </Typography>
                                                            </Box>
                                                        )}
                                                    </Box>

                                                    <Box
                                                        sx={{
                                                            p: 1.6,
                                                            borderRadius: '1.15rem',
                                                            border: `1px solid ${alpha(theme.palette.success.main, 0.16)}`,
                                                            backgroundColor: alpha(theme.palette.success.light, 0.08)
                                                        }}
                                                    >
                                                        <Typography variant="overline" sx={{display: 'block', fontWeight: 900, letterSpacing: '0.12em', color: 'text.secondary'}}>
                                                            Catalog outcome
                                                        </Typography>
                                                        <Stack spacing={1} sx={{mt: 1}}>
                                                            <Box>
                                                                <Typography variant="caption" sx={{display: 'block', color: 'text.secondary', fontWeight: 800}}>
                                                                    Existing linked catalog entry
                                                                </Typography>
                                                                <Typography sx={{mt: 0.35, fontWeight: 700}}>
                                                                    {submission.createdGameName || 'No catalog entry linked yet'}
                                                                </Typography>
                                                            </Box>
                                                            {submission.reviewedByUsername && (
                                                                <Box>
                                                                    <Typography variant="caption" sx={{display: 'block', color: 'text.secondary', fontWeight: 800}}>
                                                                        Last reviewed by
                                                                    </Typography>
                                                                    <Typography sx={{mt: 0.35, fontWeight: 700}}>
                                                                        @{submission.reviewedByUsername}
                                                                    </Typography>
                                                                </Box>
                                                            )}
                                                        </Stack>
                                                    </Box>
                                                </Stack>
                                            </Box>
                                        </Paper>
                                    );
                                })}

                                <Box ref={submissionsSentinelRef} sx={{display: 'flex', justifyContent: 'center', py: submissions.length > 0 ? 1 : 0}}>
                                    {submissionsLoadingMore ? (
                                        <CircularProgress size={24}/>
                                    ) : submissionsHasMore ? (
                                        <Typography color="text.secondary">Scroll for more submissions</Typography>
                                    ) : submissions.length > 0 ? (
                                        <Typography color="text.secondary">You’ve reached the end of the submission queue.</Typography>
                                    ) : null}
                                </Box>
                            </Stack>
                        </Paper>
                    )}

                    {activeTab === 'users' && (
                        <Paper sx={{p: 3, borderRadius: '1.5rem'}}>
                            <Stack spacing={2.5}>
                                <Stack direction={{xs: 'column', md: 'row'}} spacing={2} justifyContent="space-between">
                                    <Typography variant="h4" fontWeight={900}>Users</Typography>
                                    <Stack direction={{xs: 'column', md: 'row'}} spacing={1.5}>
                                        <TextField
                                            label="Search users"
                                            value={userQuery}
                                            onChange={(event) => setUserQuery(event.target.value)}
                                        />
                                        <Button variant="outlined" onClick={() => loadUsers().catch(() => null)}>Refresh</Button>
                                    </Stack>
                                </Stack>

                                {filteredUsers.map(user => (
                                    <Paper key={user.id} variant="outlined" sx={{p: 2.5, borderRadius: '1.25rem'}}>
                                        <Stack direction={{xs: 'column', lg: 'row'}} spacing={2} justifyContent="space-between">
                                            <Box sx={{minWidth: 0}}>
                                                <Typography variant="h6" fontWeight={800}>{user.displayName || user.username}</Typography>
                                                <Typography color="text.secondary">@{user.username} • {user.email}</Typography>
                                                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{mt: 1}}>
                                                    {user.roles.map(role => (
                                                        <Chip key={role} label={role} size="small"/>
                                                    ))}
                                                    {user.favoritePlatform && <Chip label={user.favoritePlatform} size="small" color="secondary"/>}
                                                    {user.banned && <Chip label={user.permanentBan ? 'Permanently banned' : 'Temporarily banned'} size="small" color="error"/>}
                                                </Stack>
                                                {user.banned && (
                                                    <Typography variant="body2" color="text.secondary" sx={{mt: 1}}>
                                                        {user.banReason || 'No ban reason supplied'}{user.banExpiresAt ? ` • expires ${formatDateTime(user.banExpiresAt)}` : ''}
                                                    </Typography>
                                                )}
                                            </Box>
                                            <Stack direction={{xs: 'column', sm: 'row'}} spacing={1.5}>
                                                {user.banned ? (
                                                    <Button
                                                        variant="contained"
                                                        color="success"
                                                        startIcon={<GavelOutlinedIcon/>}
                                                        disabled={busyKey === `unban:${user.id}`}
                                                        onClick={() => handleUnbanUser(user.id)}
                                                    >
                                                        {busyKey === `unban:${user.id}` ? 'Unbanning' : 'Unban'}
                                                    </Button>
                                                ) : (
                                                    <Button
                                                        variant="outlined"
                                                        color="error"
                                                        startIcon={<BlockIcon/>}
                                                        onClick={() => {
                                                            setBanTargetUser(user);
                                                            setBanDialogOpen(true);
                                                        }}
                                                    >
                                                        Ban
                                                    </Button>
                                                )}
                                            </Stack>
                                        </Stack>
                                    </Paper>
                                ))}
                            </Stack>
                        </Paper>
                    )}

                    {activeTab === 'banned' && (
                        <Paper sx={{p: 3, borderRadius: '1.5rem'}}>
                            <Stack spacing={2}>
                                <Typography variant="h4" fontWeight={900}>Banned users</Typography>
                                {bannedUsers.length === 0 ? (
                                    <Typography color="text.secondary">No active bans.</Typography>
                                ) : bannedUsers.map(user => (
                                    <Paper key={user.id} variant="outlined" sx={{p: 2.25, borderRadius: '1.25rem'}}>
                                        <Stack direction={{xs: 'column', md: 'row'}} justifyContent="space-between" spacing={2}>
                                            <Box>
                                                <Typography variant="h6" fontWeight={800}>{user.displayName || user.username}</Typography>
                                                <Typography color="text.secondary">@{user.username} • {user.email}</Typography>
                                                <Typography variant="body2" color="text.secondary" sx={{mt: 1}}>
                                                    {user.permanentBan ? 'Permanent ban' : `Banned until ${formatDateTime(user.banExpiresAt)}`}
                                                </Typography>
                                                {user.banReason && (
                                                    <Typography variant="body2" color="text.secondary">{user.banReason}</Typography>
                                                )}
                                            </Box>
                                            <Box>
                                                <Button variant="contained" color="success" onClick={() => handleUnbanUser(user.id)}>
                                                    Unban
                                                </Button>
                                            </Box>
                                        </Stack>
                                    </Paper>
                                ))}
                            </Stack>
                        </Paper>
                    )}

                    {activeTab === 'catalog' && (
                        <Paper sx={{p: 3, borderRadius: '1.5rem'}}>
                            <Stack spacing={2.5}>
                                <Stack direction={{xs: 'column', lg: 'row'}} spacing={2} justifyContent="space-between">
                                    <Typography variant="h4" fontWeight={900}>Catalog manager</Typography>
                                    <Stack direction={{xs: 'column', md: 'row'}} spacing={1.5}>
                                        <TextField
                                            label="Search catalog"
                                            value={gameQuery}
                                            onChange={(event) => setGameQuery(event.target.value)}
                                        />
                                        <Button variant="outlined" onClick={() => loadGames().catch(() => null)}>Search</Button>
                                        <Button variant="contained" startIcon={<LibraryAddOutlinedIcon/>} onClick={openCreateGameDialog}>
                                            Add game
                                        </Button>
                                    </Stack>
                                </Stack>

                                {games.length === 0 ? (
                                    <Typography color="text.secondary">No games matched that query.</Typography>
                                ) : games.map(game => {
                                    const gamePlatformLabels = splitDelimitedValues(game.platforms);
                                    const gameCategoryLabels = splitDelimitedValues(game.categories);

                                    return (
                                    <Paper key={game.id} variant="outlined" sx={{p: 2.25, borderRadius: '1.25rem'}}>
                                        <Stack direction={{xs: 'column', xl: 'row'}} justifyContent="space-between" spacing={2}>
                                            <Box sx={{minWidth: 0}}>
                                                <Typography variant="h6" fontWeight={800}>{game.name}</Typography>
                                                <Typography color="text.secondary">
                                                    {game.providerName || 'Unknown publisher'}
                                                </Typography>
                                                <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{mt: 1}}>
                                                    {game.catalogSources && <Chip label={game.catalogSources} size="small"/>}
                                                    {(game.syncProtectedFields || []).length > 0 && (
                                                        <Chip label={`Sync protected: ${(game.syncProtectedFields || []).join(', ')}`} size="small" color="secondary"/>
                                                    )}
                                                    <Chip label={`Community ${formatTenPointRating(game.averageRating, 'Unrated')}`} size="small"/>
                                                </Stack>
                                                {gamePlatformLabels.length > 0 && (
                                                    <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{mt: 1.25}}>
                                                        {gamePlatformLabels.map(platform => (
                                                            <Chip
                                                                key={`${game.id}-platform-${platform}`}
                                                                label={platform}
                                                                size="small"
                                                                variant="filled"
                                                                sx={{
                                                                    fontWeight: 800,
                                                                    color: '#8a4b00',
                                                                    border: `1px solid ${alpha(theme.palette.warning.main, 0.28)}`,
                                                                    backgroundColor: alpha(theme.palette.warning.main, 0.16),
                                                                    '& .MuiChip-label': {color: '#8a4b00'}
                                                                }}
                                                            />
                                                        ))}
                                                    </Stack>
                                                )}
                                                {gameCategoryLabels.length > 0 && (
                                                    <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap sx={{mt: 1}}>
                                                        {gameCategoryLabels.map(category => (
                                                            <Chip
                                                                key={`${game.id}-category-${category}`}
                                                                label={category}
                                                                size="small"
                                                                variant="filled"
                                                                sx={{
                                                                    fontWeight: 800,
                                                                    color: '#4c1d95',
                                                                    border: `1px solid ${alpha(theme.palette.secondary.main, 0.38)}`,
                                                                    backgroundColor: 'rgba(196, 181, 253, 0.9)',
                                                                    boxShadow: `0 6px 14px ${alpha(theme.palette.secondary.main, 0.12)}`,
                                                                    '& .MuiChip-label': {
                                                                        color: '#4c1d95',
                                                                        px: 0.4
                                                                    }
                                                                }}
                                                            />
                                                        ))}
                                                    </Stack>
                                                )}
                                                {game.description && (
                                                    <Typography variant="body2" color="text.secondary" sx={{mt: 1.25}}>
                                                        {game.description}
                                                    </Typography>
                                                )}
                                            </Box>
                                            <Stack direction={{xs: 'column', sm: 'row'}} spacing={1.5}>
                                                {dashboard?.spotlightGameId === game.id ? (
                                                    <Chip
                                                        icon={<AutoAwesomeIcon/>}
                                                        label="Current spotlight"
                                                        color="secondary"
                                                        sx={{fontWeight: 800}}
                                                    />
                                                ) : (
                                                    <Button
                                                        variant="contained"
                                                        color="secondary"
                                                        startIcon={<AutoAwesomeIcon/>}
                                                        disabled={busyKey === `spotlight:${game.id}`}
                                                        onClick={() => handleSetSpotlight(game)}
                                                    >
                                                        {busyKey === `spotlight:${game.id}` ? 'Setting spotlight' : 'Set spotlight'}
                                                    </Button>
                                                )}
                                                <Button variant="outlined" startIcon={<EditOutlinedIcon/>} onClick={() => openEditGameDialog(game)}>
                                                    Edit
                                                </Button>
                                                <Button variant="outlined" color="error" onClick={() => handleDeleteGame(game.id)}>
                                                    Delete
                                                </Button>
                                            </Stack>
                                        </Stack>
                                    </Paper>
                                    );
                                })}
                            </Stack>
                        </Paper>
                    )}

                    {activeTab === 'jobs' && (
                        <Paper sx={{p: 3, borderRadius: '1.5rem'}}>
                            <Stack spacing={2.25}>
                                <Stack direction={{xs: 'column', lg: 'row'}} spacing={2} justifyContent="space-between">
                                    <Box>
                                        <Typography variant="h4" fontWeight={900}>System jobs</Typography>
                                        <Typography color="text.secondary" sx={{mt: 0.75}}>
                                            Background sync and maintenance work now runs as tracked jobs with durable progress instead of one-off request execution.
                                        </Typography>
                                    </Box>
                                    <Stack direction={{xs: 'column', md: 'row'}} spacing={1.25}>
                                        <Button
                                            variant="contained"
                                            onClick={() => handleQueueJob('CATALOG_SYNC', 'PLAYSTATION_STORE')}
                                            disabled={busyKey === 'job:CATALOG_SYNC:PLAYSTATION_STORE'}
                                        >
                                            Queue PSN sync
                                        </Button>
                                        <Button
                                            variant="contained"
                                            color="secondary"
                                            onClick={() => handleQueueJob('CATALOG_SYNC', 'IGDB_PC_XBOX')}
                                            disabled={busyKey === 'job:CATALOG_SYNC:IGDB_PC_XBOX'}
                                        >
                                            Queue PC/Xbox sync
                                        </Button>
                                        <Button
                                            variant="outlined"
                                            onClick={() => handleQueueJob('CATALOG_CONSOLIDATION')}
                                            disabled={busyKey === 'job:CATALOG_CONSOLIDATION'}
                                        >
                                            Queue consolidation
                                        </Button>
                                        <Button
                                            variant="outlined"
                                            color="secondary"
                                            onClick={() => handleQueueJob('GAME_STATS_REBUILD')}
                                            disabled={busyKey === 'job:GAME_STATS_REBUILD'}
                                        >
                                            Queue stats rebuild
                                        </Button>
                                        <Button variant="outlined" onClick={() => loadJobs().catch(() => null)}>
                                            Refresh
                                        </Button>
                                    </Stack>
                                </Stack>

                                {jobs.length === 0 ? (
                                    <Typography color="text.secondary">No jobs have been queued yet.</Typography>
                                ) : jobs.map(job => {
                                    const statusColor = job.status === 'SUCCESS'
                                        ? theme.palette.success.main
                                        : job.status === 'FAILED'
                                            ? theme.palette.error.main
                                            : job.status === 'SKIPPED'
                                                ? theme.palette.warning.main
                                                : theme.palette.info.main;

                                    return (
                                        <Paper
                                            key={job.id}
                                            variant="outlined"
                                            sx={{
                                                p: 2.25,
                                                borderRadius: '1.25rem',
                                                borderColor: alpha(statusColor, 0.24),
                                                background: `linear-gradient(180deg, ${alpha(theme.palette.background.paper, 0.98)} 0%, ${alpha(statusColor, 0.05)} 100%)`
                                            }}
                                        >
                                            <Stack spacing={1.25}>
                                                <Stack direction={{xs: 'column', md: 'row'}} spacing={1.25} justifyContent="space-between">
                                                    <Box>
                                                        <Stack direction="row" spacing={1} useFlexGap sx={{flexWrap: 'wrap'}}>
                                                            <Chip label={job.type.replace(/_/g, ' ')} size="small"/>
                                                            <Chip
                                                                label={job.status.replace(/_/g, ' ')}
                                                                size="small"
                                                                sx={{
                                                                    color: theme.palette.common.white,
                                                                    backgroundColor: statusColor,
                                                                    fontWeight: 800
                                                                }}
                                                            />
                                                            {job.catalogSource && (
                                                                <Chip label={job.catalogSource.replace(/_/g, ' ')} size="small" variant="outlined"/>
                                                            )}
                                                        </Stack>
                                                        <Typography variant="h6" fontWeight={800} sx={{mt: 1}}>
                                                            {job.progressMessage || job.resultSummary || 'Queued'}
                                                        </Typography>
                                                    </Box>
                                                    <Typography color="text.secondary" sx={{whiteSpace: 'nowrap'}}>
                                                        {formatDateTime(job.createdAt)}
                                                    </Typography>
                                                </Stack>

                                                <Typography color="text.secondary">
                                                    Requested by {job.requestedBy || 'system'}
                                                </Typography>

                                                {job.relatedSyncRunId && (
                                                    <Typography variant="body2" color="text.secondary">
                                                        Related sync run: {job.relatedSyncRunId}
                                                    </Typography>
                                                )}

                                                {job.errorMessage && (
                                                    <Typography variant="body2" sx={{color: theme.palette.error.main, fontWeight: 700}}>
                                                        {job.errorMessage}
                                                    </Typography>
                                                )}

                                                {(job.startedAt || job.completedAt || job.lastHeartbeatAt) && (
                                                    <Stack direction="row" spacing={1} useFlexGap sx={{flexWrap: 'wrap'}}>
                                                        {job.startedAt && <Chip size="small" label={`Started ${formatDateTime(job.startedAt)}`}/>}
                                                        {job.completedAt && <Chip size="small" label={`Completed ${formatDateTime(job.completedAt)}`}/>}
                                                        {job.lastHeartbeatAt && <Chip size="small" label={`Heartbeat ${formatDateTime(job.lastHeartbeatAt)}`}/>}
                                                    </Stack>
                                                )}
                                            </Stack>
                                        </Paper>
                                    );
                                })}
                            </Stack>
                        </Paper>
                    )}

                    {activeTab === 'audit' && (
                        <Paper sx={{p: 3, borderRadius: '1.5rem'}}>
                            <Stack spacing={2.25}>
                                <Stack direction={{xs: 'column', md: 'row'}} spacing={2} justifyContent="space-between">
                                    <Box>
                                        <Typography variant="h4" fontWeight={900}>Admin audit log</Typography>
                                        <Typography color="text.secondary" sx={{mt: 0.75}}>
                                            Recent moderation, catalog, spotlight, and submission-review actions.
                                        </Typography>
                                    </Box>
                                    <Button variant="outlined" onClick={() => loadAuditLogs().catch(() => null)}>
                                        Refresh
                                    </Button>
                                </Stack>

                                {auditLogs.length === 0 ? (
                                    <Typography color="text.secondary">No audit entries yet.</Typography>
                                ) : auditLogs.map(entry => (
                                    <Paper
                                        key={entry.id}
                                        variant="outlined"
                                        sx={{
                                            p: 2.25,
                                            borderRadius: '1.25rem',
                                            background: `linear-gradient(180deg, ${alpha(theme.palette.background.paper, 0.98)} 0%, ${alpha(theme.palette.primary.light, 0.04)} 100%)`
                                        }}
                                    >
                                        <Stack spacing={1.2}>
                                            <Stack direction={{xs: 'column', md: 'row'}} spacing={1.2} justifyContent="space-between">
                                                <Box>
                                                    <Stack direction="row" spacing={1} useFlexGap sx={{flexWrap: 'wrap'}}>
                                                        <Chip size="small" label={entry.action.replace(/_/g, ' ')} color="secondary"/>
                                                        <Chip size="small" label={entry.entityType.replace(/_/g, ' ')} variant="outlined"/>
                                                        {entry.entityId && <Chip size="small" label={`Target ${entry.entityId}`} variant="outlined"/>}
                                                    </Stack>
                                                    <Typography variant="h6" fontWeight={800} sx={{mt: 1}}>
                                                        {entry.summary}
                                                    </Typography>
                                                </Box>
                                                <Typography color="text.secondary" sx={{whiteSpace: 'nowrap'}}>
                                                    {formatDateTime(entry.createdAt)}
                                                </Typography>
                                            </Stack>
                                            <Typography color="text.secondary">
                                                {entry.actorUsername ? `by @${entry.actorUsername}` : 'System action'}
                                            </Typography>
                                            {entry.details && Object.keys(entry.details).length > 0 && (
                                                <Stack direction="row" spacing={1} useFlexGap sx={{flexWrap: 'wrap'}}>
                                                    {Object.entries(entry.details).map(([key, value]) => (
                                                        <Chip
                                                            key={`${entry.id}:${key}`}
                                                            size="small"
                                                            label={`${key}: ${Array.isArray(value) ? value.join(', ') : String(value || '')}`}
                                                            sx={{maxWidth: '100%'}}
                                                        />
                                                    ))}
                                                </Stack>
                                            )}
                                        </Stack>
                                    </Paper>
                                ))}
                            </Stack>
                        </Paper>
                    )}
                </Stack>
            </Container>

            <Dialog open={banDialogOpen} onClose={() => setBanDialogOpen(false)} maxWidth="sm" fullWidth>
                <DialogTitle>Ban user</DialogTitle>
                <DialogContent>
                    <Stack spacing={2} sx={{pt: 1}}>
                        <Typography color="text.secondary">
                            {banTargetUser ? `Choose how long ${banTargetUser.displayName || banTargetUser.username} should be banned.` : ''}
                        </Typography>
                        <TextField
                            select
                            label="Ban length"
                            value={banDuration}
                            onChange={(event) => setBanDuration(event.target.value)}
                        >
                            <MenuItem value="1">1 day</MenuItem>
                            <MenuItem value="7">7 days</MenuItem>
                            <MenuItem value="30">30 days</MenuItem>
                            <MenuItem value="custom">Custom days</MenuItem>
                            <MenuItem value="permanent">Permanent</MenuItem>
                        </TextField>
                        {banDuration === 'custom' && (
                            <TextField
                                label="Custom days"
                                value={customBanDays}
                                onChange={(event) => setCustomBanDays(event.target.value)}
                                type="number"
                            />
                        )}
                        <TextField
                            label="Reason"
                            value={banReason}
                            onChange={(event) => setBanReason(event.target.value)}
                            multiline
                            minRows={3}
                        />
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setBanDialogOpen(false)}>Cancel</Button>
                    <Button variant="contained" color="error" onClick={handleBanUser} disabled={busyKey?.startsWith('ban:')}>
                        {busyKey?.startsWith('ban:') ? 'Banning' : 'Ban user'}
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog open={editorOpen} onClose={() => setEditorOpen(false)} maxWidth="md" fullWidth>
                <DialogTitle>{gameEditor.id ? 'Edit game entry' : 'Add game entry'}</DialogTitle>
                <DialogContent>
                    <Stack spacing={2} sx={{pt: 1}}>
                        <Typography color="text.secondary">
                            Admin edits automatically mark the changed fields as protected from future sync updates.
                        </Typography>
                        {gameEditor.syncProtectedFields.length > 0 && (
                            <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                                {gameEditor.syncProtectedFields.map(field => (
                                    <Chip key={field} label={field}/>
                                ))}
                            </Stack>
                        )}
                        <Stack direction={{xs: 'column', md: 'row'}} spacing={2}>
                            <TextField
                                fullWidth
                                required
                                label="Game name"
                                value={gameEditor.name}
                                onChange={(event) => setGameEditor(previous => ({...previous, name: event.target.value}))}
                            />
                            <TextField
                                fullWidth
                                label="Publisher"
                                value={gameEditor.providerName}
                                onChange={(event) => setGameEditor(previous => ({...previous, providerName: event.target.value}))}
                            />
                        </Stack>
                        <Stack direction={{xs: 'column', md: 'row'}} spacing={2}>
                            <TextField
                                fullWidth
                                required
                                label="Platforms"
                                value={gameEditor.platforms}
                                onChange={(event) => setGameEditor(previous => ({...previous, platforms: event.target.value}))}
                            />
                            <TextField
                                fullWidth
                                label="Categories"
                                value={gameEditor.categories}
                                onChange={(event) => setGameEditor(previous => ({...previous, categories: event.target.value}))}
                            />
                        </Stack>
                        <TextField
                            fullWidth
                            multiline
                            minRows={4}
                            label="Description"
                            value={gameEditor.description}
                            onChange={(event) => setGameEditor(previous => ({...previous, description: event.target.value}))}
                        />
                        <Stack direction={{xs: 'column', md: 'row'}} spacing={2}>
                            <TextField
                                fullWidth
                                label="Release date"
                                value={gameEditor.releaseDate}
                                onChange={(event) => setGameEditor(previous => ({...previous, releaseDate: event.target.value}))}
                            />
                            <TextField
                                fullWidth
                                label="Price"
                                value={gameEditor.price}
                                onChange={(event) => setGameEditor(previous => ({...previous, price: event.target.value}))}
                            />
                        </Stack>
                        <TextField
                            fullWidth
                            label="Image URL"
                            value={gameEditor.imageUrl}
                            onChange={(event) => setGameEditor(previous => ({...previous, imageUrl: event.target.value}))}
                        />
                        <TextField
                            fullWidth
                            label="Store / reference URL"
                            value={gameEditor.url}
                            onChange={(event) => setGameEditor(previous => ({...previous, url: event.target.value}))}
                        />
                    </Stack>
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setEditorOpen(false)}>Cancel</Button>
                    <Button variant="contained" onClick={handleSaveGame} disabled={busyKey === (gameEditor.id ? `save:${gameEditor.id}` : 'create:game')}>
                        {busyKey === (gameEditor.id ? `save:${gameEditor.id}` : 'create:game') ? 'Saving' : 'Save'}
                    </Button>
                </DialogActions>
            </Dialog>
        </Box>
    );
}

export default AdminPortalPage;
