import React, {useEffect} from "react";
import {AppBar, Box, Button, Container, IconButton, Menu, MenuItem, Toolbar, Tooltip, Typography} from "@mui/material";
import AdbIcon from '@mui/icons-material/Adb';
import MenuIcon from '@mui/icons-material/Menu';
import axios from "axios";
import {useNavigate} from "react-router-dom";
import PopupMessage from "../GeneralComponents/PopupMessage";
import {AccountCircle} from "@mui/icons-material";
import SearchIcon from '@mui/icons-material/Search';
import QuickSearchPage from "./QuickSearchPage";


const pages = ['Home', 'Random Game', 'My List', 'Browse'];

const settings = ['Profile', 'Account', 'Dashboard', 'Logout'];


function ResponsiveAppBar() {
    const [anchorElNav, setAnchorElNav] = React.useState<null | HTMLElement>(null);
    const [anchorElUser, setAnchorElUser] = React.useState<null | HTMLElement>(null);
    const [errorMessage, setErrorMessage] = React.useState('');
    const [searchModalOpen, setSearchModalOpen] = React.useState(false);

    const navigate = useNavigate();

    const handleOpenNavMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
        setAnchorElNav(event.currentTarget);
    }

    const handleOpenUserMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
        setAnchorElUser(event.currentTarget);
    }

    const handleCloseNavMenu = () => {
        setAnchorElNav(null);
    };

    const handleCloseUserMenu = () => {
        setAnchorElUser(null);
    };

    const handleLogOut = () => {
        axios.post("/api/auth/logout")
            .then(response => {
                navigate('/login');
            })
            .catch(error => {
                    setErrorMessage(error.response.data.message || error.message);

                }
            );
    }

    const handleOpenSearchModal = () => {
        setSearchModalOpen(true);
    }
    const handleCloseSearchModal = () => {
        setSearchModalOpen(false);
    }

    const handleRandomGame = () => {

        axios.get("/api/auth/games/random-id")
            .then(response => {
                if (!response || !response.data) {
                    setErrorMessage("No games found");
                    return null;
                } else {
                    navigate('/games/' + response.data.id);
                }
            })
            .catch(error => {
                    console.log(error)
                    setErrorMessage((error.response && error.response.data && error.response.data.message) || error.message);
                }
            );
    }
    const handleNavigation = (page: string) => {
        console.log("Navigating to " + page + " page");
        switch (page) {
            case 'Home':
                navigate('/');
                break;
            case 'Random Game':
                handleRandomGame();
                break;
            case 'My List':
                navigate('/mylist');
                break;
            case 'Browse':
                navigate('/search');
                break;
            default:
                break;
        }
        handleCloseNavMenu();
    }

    const handleSetting = (setting: string) => {
        switch (setting) {
            case 'Profile':
                navigate('/profile');
                break;
            case 'Account':
                navigate('/account');
                break;
            case 'Dashboard':
                navigate('/dashboard');
                break;
            case 'Logout':
                handleLogOut();
                break;
            default:
                break;
        }
        handleCloseUserMenu();
    }

    useEffect(() => {
        function handleKeyPress(event: KeyboardEvent) {
            if (event.ctrlKey && event.key === 's') {
                event.preventDefault();
                handleOpenSearchModal();
            }
        }

        window.addEventListener('keydown', handleKeyPress);

        return () => {
            window.removeEventListener('keydown', handleKeyPress);
        }
    }, []);

    return (

        // <HideOnScroll>
        <AppBar position="fixed">
            <PopupMessage message={errorMessage}/>
            <Container maxWidth="xl">
                <Toolbar disableGutters>
                    <AdbIcon sx={{display: {xs: 'none', md: 'flex'}, mr: 1}}/>
                    <Typography
                        variant="h6"
                        noWrap
                        component="a"
                        href="/api"
                        sx={{
                            mr: 2,
                            display: {xs: 'none', md: 'flex'},
                            fontFamily: 'monospace',
                            fontWeight: 700,
                            letterSpacing: '.3rem',
                            color: 'inherit',
                            textDecoration: 'none',
                        }}
                    >
                        LOGO
                    </Typography>
                    <Box sx={{flexGrow: 1, display: {xs: 'flex', md: 'none'}}}>
                        <IconButton
                            size="large"
                            aria-label="account of current user"
                            aria-controls="menu-appbar"
                            aria-haspopup="true"
                            onClick={handleOpenNavMenu}
                            color="inherit"
                        >
                            <MenuIcon/>
                        </IconButton>
                        <Menu
                            id="menu-appbar"
                            anchorEl={anchorElNav}
                            anchorOrigin={{
                                vertical: 'bottom',
                                horizontal: 'left',
                            }}
                            keepMounted
                            transformOrigin={{
                                vertical: 'top',
                                horizontal: 'left',
                            }}
                            open={Boolean(anchorElNav)}
                            onClose={handleCloseNavMenu}
                            sx={{
                                display: {xs: 'block', md: 'none'},
                            }}
                        >
                            {pages.map((page) => (
                                <MenuItem key={page} onClick={() => handleNavigation(page)}>
                                    <Typography textAlign="center">{page}</Typography>
                                </MenuItem>
                            ))}
                        </Menu>

                    </Box>
                    <AdbIcon sx={{display: {xs: 'flex', md: 'none'}, mr: 1}}/>
                    <Typography
                        variant="h5"
                        noWrap
                        component="a"
                        href="/"
                        sx={{
                            mr: 2,
                            flexGrow: 1,
                            display: {xs: 'flex', md: 'none'},
                            fontFamily: 'monospace',
                            fontWeight: 700,
                            letterSpacing: '.3rem',
                            color: 'inherit',
                            textDecoration: 'none',
                        }}
                    >
                        LOGO
                    </Typography>
                    <Box sx={{flexGrow: 1, display: {xs: 'none', md: 'flex'}}}>
                        {pages.map((page) => (
                            <Button
                                key={page}
                                onClick={() => handleNavigation(page)}
                                sx={{my: 2, color: 'white', display: 'block'}}
                            >
                                {page}
                            </Button>
                        ))}
                    </Box>
                    <IconButton size="large" color="inherit" onClick={handleOpenSearchModal}>
                        <SearchIcon/>
                    </IconButton>
                    <Box sx={{flexGrow: 0}}>
                        <Tooltip title="Open settings">
                            <IconButton onClick={handleOpenUserMenu} size="large"
                                        color="inherit">
                                <AccountCircle/>
                            </IconButton>
                        </Tooltip>
                        <Menu
                            sx={{mt: '45px'}}
                            id="menu-appbar"
                            anchorEl={anchorElUser}
                            anchorOrigin={{
                                vertical: 'top',
                                horizontal: 'right',
                            }}
                            keepMounted
                            transformOrigin={{
                                vertical: 'top',
                                horizontal: 'right',
                            }}
                            open={Boolean(anchorElUser)}
                            onClose={handleCloseUserMenu}
                        >
                            {settings.map((setting) => (
                                <MenuItem key={setting} onClick={() => handleSetting(setting)}>
                                    <Typography textAlign="center">{setting}</Typography>
                                </MenuItem>
                            ))}
                        </Menu>
                    </Box>
                </Toolbar>
            </Container>
            <QuickSearchPage
                open={searchModalOpen}
                onClose={handleCloseSearchModal}
            />

        </AppBar>
        // </HideOnScroll>
    )
}

export default ResponsiveAppBar;