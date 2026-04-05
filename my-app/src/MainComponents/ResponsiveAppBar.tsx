import React, {useEffect} from "react";
import {
    AppBar,
    Avatar,
    Box,
    Button,
    Container,
    IconButton,
    Menu,
    MenuItem,
    Toolbar,
    Tooltip,
    Typography
} from "@mui/material";
import SportsEsportsIcon from '@mui/icons-material/SportsEsports';
import MenuIcon from '@mui/icons-material/Menu';
import SearchIcon from '@mui/icons-material/Search';
import ShuffleIcon from '@mui/icons-material/Shuffle';
import {AccountCircle} from "@mui/icons-material";
import {useNavigate} from "react-router-dom";
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {UserProfile} from "../GeneralComponents/types/UserProfile";
import QuickSearchPage from "./QuickSearchPage";
import {getErrorMessage} from "../api/client";
import {getMyProfile, getRandomGameId, logout} from "../api/games";

const PROFILE_UPDATED_EVENT = 'gamesdb:profile-updated';
const ADMIN_CACHE_KEY = 'gamesdb:is-admin';
const PROFILE_CACHE_KEY = 'gamesdb:profile-cache';

const navigationItems = ['Home', 'Search', 'Discover', 'Random Game', 'My List'];

function isAdminProfile(profile?: UserProfile | null): boolean {
    return !!profile?.admin || !!profile?.roles?.includes('ADMIN');
}

function ResponsiveAppBar() {
    const [anchorElNav, setAnchorElNav] = React.useState<null | HTMLElement>(null);
    const [anchorElUser, setAnchorElUser] = React.useState<null | HTMLElement>(null);
    const [message, setMessage] = React.useState('');
    const [severity, setSeverity] = React.useState(Severity.ERROR);
    const [searchModalOpen, setSearchModalOpen] = React.useState(false);
    const [profile, setProfile] = React.useState<UserProfile | null>(null);

    const navigate = useNavigate();

    const handleNavigation = async (item: string) => {
        setAnchorElNav(null);

        if (item === 'Home') {
            navigate('/');
            return;
        }

        if (item === 'Discover') {
            navigate('/discover');
            return;
        }

        if (item === 'Search') {
            navigate('/search');
            return;
        }

        if (item === 'My List') {
            navigate('/mylist');
            return;
        }

        if (item === 'Random Game') {
            try {
                const gameId = await getRandomGameId();
                navigate(`/games/${gameId}`);
            } catch (error) {
                setMessage(getErrorMessage(error));
                setSeverity(Severity.ERROR);
            }
        }
    };

    const handleLogOut = async () => {
        try {
            await logout();
            if (typeof window !== 'undefined') {
                window.localStorage.removeItem(ADMIN_CACHE_KEY);
                window.localStorage.removeItem(PROFILE_CACHE_KEY);
            }
            navigate('/login');
        } catch (error) {
            setMessage(getErrorMessage(error));
            setSeverity(Severity.ERROR);
        } finally {
            setAnchorElUser(null);
        }
    };

    const handleSettingsOpen = () => {
        setAnchorElUser(null);
        navigate('/settings');
    };

    const handleMyListOpen = () => {
        setAnchorElUser(null);
        navigate('/mylist');
    };

    const handleSuggestGameOpen = () => {
        setAnchorElUser(null);
        navigate('/submit-game');
    };

    const handleAdminOpen = () => {
        setAnchorElUser(null);
        navigate('/admin');
    };

    useEffect(() => {
        function handleKeyPress(event: KeyboardEvent) {
            if (event.ctrlKey && event.key.toLowerCase() === 's') {
                event.preventDefault();
                setSearchModalOpen(true);
            }
        }

        function handleProfileUpdated(event: Event) {
            const customEvent = event as CustomEvent<UserProfile>;
            if (customEvent.detail) {
                setProfile(customEvent.detail);
                if (typeof window !== 'undefined') {
                    window.localStorage.setItem(ADMIN_CACHE_KEY, String(isAdminProfile(customEvent.detail)));
                    window.localStorage.setItem(PROFILE_CACHE_KEY, JSON.stringify(customEvent.detail));
                }
            }
        }

        if (typeof window !== 'undefined') {
            const cachedProfile = window.localStorage.getItem(PROFILE_CACHE_KEY);
            if (cachedProfile) {
                try {
                    setProfile(JSON.parse(cachedProfile));
                } catch (_error) {
                    window.localStorage.removeItem(PROFILE_CACHE_KEY);
                }
            }
        }

        getMyProfile().then(nextProfile => {
            setProfile(nextProfile);
            if (typeof window !== 'undefined') {
                window.localStorage.setItem(ADMIN_CACHE_KEY, String(isAdminProfile(nextProfile)));
                window.localStorage.setItem(PROFILE_CACHE_KEY, JSON.stringify(nextProfile));
            }
        }).catch(() => null);
        window.addEventListener('keydown', handleKeyPress);
        window.addEventListener(PROFILE_UPDATED_EVENT, handleProfileUpdated as EventListener);
        return () => {
            window.removeEventListener('keydown', handleKeyPress);
            window.removeEventListener(PROFILE_UPDATED_EVENT, handleProfileUpdated as EventListener);
        };
    }, []);

    const avatarFallback = (profile?.displayName || profile?.username || 'G').slice(0, 1).toUpperCase();
    const cachedAdmin = typeof window !== 'undefined' && window.localStorage.getItem(ADMIN_CACHE_KEY) === 'true';
    const isAdmin = isAdminProfile(profile) || cachedAdmin;

    return (
        <AppBar
            position="fixed"
            sx={{
                background: 'linear-gradient(90deg, rgba(51,24,97,0.96), rgba(106,58,177,0.92))',
                boxShadow: '0 20px 50px rgba(24, 7, 51, 0.35)'
            }}
        >
            <PopupMessage message={message} severity={severity}/>
            <Container maxWidth="xl">
                <Toolbar disableGutters>
                    <SportsEsportsIcon sx={{display: {xs: 'none', md: 'flex'}, mr: 1.5}}/>
                    <Typography
                        variant="h6"
                        component="button"
                        onClick={() => navigate('/')}
                        sx={{
                            mr: 3,
                            display: {xs: 'none', md: 'flex'},
                            fontFamily: '"Trebuchet MS", "Segoe UI", sans-serif',
                            fontWeight: 800,
                            letterSpacing: '0.08em',
                            color: 'inherit',
                            border: 0,
                            background: 'transparent'
                        }}
                    >
                        Games DB
                    </Typography>

                    <Box sx={{flexGrow: 1, display: {xs: 'flex', md: 'none'}}}>
                        <IconButton size="large" color="inherit" onClick={(event) => setAnchorElNav(event.currentTarget)}>
                            <MenuIcon/>
                        </IconButton>
                        <Menu
                            anchorEl={anchorElNav}
                            anchorOrigin={{vertical: 'bottom', horizontal: 'left'}}
                            transformOrigin={{vertical: 'top', horizontal: 'left'}}
                            open={Boolean(anchorElNav)}
                            onClose={() => setAnchorElNav(null)}
                            sx={{display: {xs: 'block', md: 'none'}}}
                        >
                            {navigationItems.map(item => (
                                <MenuItem key={item} onClick={() => handleNavigation(item)}>
                                    <Typography textAlign="center">{item}</Typography>
                                </MenuItem>
                            ))}
                        </Menu>
                    </Box>

                    <SportsEsportsIcon sx={{display: {xs: 'flex', md: 'none'}, mr: 1}}/>
                    <Typography
                        variant="h5"
                        component="button"
                        onClick={() => navigate('/')}
                        sx={{
                            mr: 2,
                            flexGrow: 1,
                            display: {xs: 'flex', md: 'none'},
                            fontFamily: '"Trebuchet MS", "Segoe UI", sans-serif',
                            fontWeight: 800,
                            letterSpacing: '0.05em',
                            color: 'inherit',
                            border: 0,
                            background: 'transparent'
                        }}
                    >
                        Games DB
                    </Typography>

                    <Box sx={{flexGrow: 1, display: {xs: 'none', md: 'flex'}, gap: 1}}>
                        {navigationItems.map(item => (
                            <Button
                                key={item}
                                onClick={() => handleNavigation(item)}
                                startIcon={item === 'Random Game' ? <ShuffleIcon/> : undefined}
                                sx={{my: 2, color: 'white', display: 'flex'}}
                            >
                                {item}
                            </Button>
                        ))}
                    </Box>

                    <Tooltip title="Quick search">
                        <IconButton size="large" color="inherit" onClick={() => setSearchModalOpen(true)}>
                            <SearchIcon/>
                        </IconButton>
                    </Tooltip>

                    <Box sx={{flexGrow: 0}}>
                        <Tooltip title="Account">
                            <IconButton onClick={(event) => setAnchorElUser(event.currentTarget)} size="large" color="inherit">
                                {profile ? (
                                    <Avatar
                                        src={profile.avatarUrl || undefined}
                                        sx={{width: 36, height: 36, bgcolor: 'rgba(255,255,255,0.18)', color: 'white'}}
                                    >
                                        {avatarFallback}
                                    </Avatar>
                                ) : (
                                    <AccountCircle/>
                                )}
                            </IconButton>
                        </Tooltip>
                        <Menu
                            sx={{mt: '45px'}}
                            anchorEl={anchorElUser}
                            anchorOrigin={{vertical: 'top', horizontal: 'right'}}
                            transformOrigin={{vertical: 'top', horizontal: 'right'}}
                            open={Boolean(anchorElUser)}
                            onClose={() => setAnchorElUser(null)}
                        >
                            {profile && (
                                <MenuItem disabled>
                                    <Box>
                                        <Typography fontWeight={700}>{profile.displayName || profile.username}</Typography>
                                        <Typography variant="body2" color="text.secondary">{profile.email}</Typography>
                                    </Box>
                                </MenuItem>
                            )}
                            <MenuItem onClick={handleMyListOpen}>
                                <Typography textAlign="center">My List</Typography>
                            </MenuItem>
                            <MenuItem onClick={handleSuggestGameOpen}>
                                <Typography textAlign="center">Suggest Game</Typography>
                            </MenuItem>
                            {isAdmin && (
                                <MenuItem onClick={handleAdminOpen}>
                                    <Typography textAlign="center">Admin Portal</Typography>
                                </MenuItem>
                            )}
                            <MenuItem onClick={handleSettingsOpen}>
                                <Typography textAlign="center">Settings</Typography>
                            </MenuItem>
                            <MenuItem onClick={handleLogOut}>
                                <Typography textAlign="center">Logout</Typography>
                            </MenuItem>
                        </Menu>
                    </Box>
                </Toolbar>
            </Container>

            <QuickSearchPage open={searchModalOpen} onClose={() => setSearchModalOpen(false)}/>
        </AppBar>
    );
}

export default ResponsiveAppBar;
