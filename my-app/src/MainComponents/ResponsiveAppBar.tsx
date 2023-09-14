import React from "react";
import {
    AppBar,
    Avatar,
    Box,
    Button,
    Container,
    IconButton,
    Menu,
    MenuItem,
    Slide,
    Toolbar,
    Tooltip,
    Typography,
    useScrollTrigger
} from "@mui/material";
import AdbIcon from '@mui/icons-material/Adb';
import MenuIcon from '@mui/icons-material/Menu';
import axios from "axios";
import {useNavigate} from "react-router-dom";
import PopupMessage from "../GeneralComponents/PopupMessage";


const pages = ['Home', 'Random Game', 'My List'];

const settings = ['Profile', 'Account', 'Dashboard', 'Logout'];


function ResponsiveAppBar() {
    const [anchorElNav, setAnchorElNav] = React.useState<null | HTMLElement>(null);
    const [anchorElUser, setAnchorElUser] = React.useState<null | HTMLElement>(null);
    const [errorMessage, setErrorMessage] = React.useState('');
    // const [gameId, setGameId] = React.useState('' as any);

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


    function HideOnScroll(props: { children: React.ReactElement }) {
        const {children} = props;
        const trigger = useScrollTrigger();
        console.log(trigger)


        return (
            <Slide appear={false} direction="down" in={!trigger}>
                {children}
            </Slide>
        );
    }

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

                    <Box sx={{flexGrow: 0}}>
                        <Tooltip title="Open settings">
                            <IconButton onClick={handleOpenUserMenu} sx={{p: 0}}>
                                <Avatar alt="Remy Sharp" src="/static/images/avatar/2.jpg"/>
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
        </AppBar>
        // </HideOnScroll>
    )
}

export default ResponsiveAppBar;