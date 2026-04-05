import {
    alpha,
    Avatar,
    Button,
    Container,
    debounce,
    InputBase,
    List,
    ListItem,
    ListItemAvatar,
    ListItemText,
    Modal,
    styled,
    Typography
} from "@mui/material";
import React, {useEffect, useState} from "react";
import SearchIcon from "@mui/icons-material/Search";
import OpenInFullIcon from "@mui/icons-material/OpenInFull";
import {Link, useNavigate} from "react-router-dom";
import {GameData} from "../GeneralComponents/types/GameData";
import {ModalMainProps} from "../GeneralComponents/types/ModalMainProps";
import {getErrorMessage} from "../api/client";
import {searchGames} from "../api/games";

const Search = styled('div')(({theme}) => ({
    position: 'relative',
    borderRadius: '1.2em',
    backgroundColor: alpha(theme.palette.common.white, 0.92),
    width: '100%',
    boxShadow: '0 24px 60px rgba(15, 5, 37, 0.28)'
}));

const SearchIconWrapper = styled('div')(({theme}) => ({
    padding: theme.spacing(0, 2),
    height: '100%',
    position: 'absolute',
    pointerEvents: 'none',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center'
}));

const StyledInputBase = styled(InputBase)(({theme}) => ({
    color: 'inherit',
    width: '100%',
    '& .MuiInputBase-input': {
        padding: theme.spacing(2, 1, 2, 0),
        paddingLeft: `calc(1em + ${theme.spacing(4)})`,
        width: '100%',
        '&::placeholder': {
            color: 'rgb(82,82,101)',
            fontStyle: 'italic',
            opacity: 1,
            fontWeight: 700
        }
    }
}));

const HoveredListItem = styled(ListItem)(({theme}) => ({
    transition: 'background-color 180ms ease, transform 180ms ease',
    '&:hover': {
        backgroundColor: alpha(theme.palette.primary.main, 0.08),
        transform: 'translateY(-1px)'
    }
}));

const ControlNotice = styled(Typography)(({theme}) => ({
    color: 'white',
    textAlign: 'right',
    fontSize: '0.8rem',
    marginTop: theme.spacing(1),
    fontWeight: 700
}));

const QuickSearchPage: React.FC<ModalMainProps> = ({open, onClose}) => {
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<GameData[]>([]);
    const [message, setMessage] = useState('');
    const searchLimit = 30;
    const navigate = useNavigate();

    const debouncedSearch = React.useMemo(() =>
            debounce(async (query: string) => {
                try {
                    const results = await searchGames(query, searchLimit);
                    setSearchResults(results);
                    setMessage(results.length === 0 ? 'No matches found.' : '');
                } catch (error) {
                    setSearchResults([]);
                    setMessage(getErrorMessage(error));
                }
            }, 250),
        []
    );

    useEffect(() => {
        if (searchQuery.trim()) {
            debouncedSearch(searchQuery.trim());
        } else {
            setSearchResults([]);
            setMessage('');
        }

        return () => debouncedSearch.clear();
    }, [searchQuery, debouncedSearch]);

    const closeAndReset = () => {
        setSearchQuery('');
        setSearchResults([]);
        setMessage('');
        onClose();
    };

    function openFullSearchPage(queryOverride = searchQuery) {
        const normalizedQuery = queryOverride.trim();
        closeAndReset();
        navigate(normalizedQuery ? `/search?q=${encodeURIComponent(normalizedQuery)}` : '/search');
    }

    function formatSecondaryText(result: GameData) {
        if (result.provider && result.platforms) {
            return `${result.provider} • ${result.platforms}`;
        }
        return result.provider || result.platforms || 'Platform unknown';
    }

    return (
        <Modal
            open={open}
            onClose={closeAndReset}
            aria-labelledby="search-modal"
            sx={{
                display: 'flex',
                alignItems: 'flex-start',
                justifyContent: 'center',
                backdropFilter: 'blur(8px)',
                padding: '10vh 1.25rem'
            }}
        >
            <Container maxWidth="md">
                <Search>
                    <SearchIconWrapper>
                        <SearchIcon/>
                    </SearchIconWrapper>
                    <StyledInputBase
                        fullWidth
                        value={searchQuery}
                        onChange={(event) => setSearchQuery(event.target.value)}
                        onKeyDown={(event) => {
                            if (event.key === 'Enter') {
                                event.preventDefault();
                                openFullSearchPage();
                            }
                        }}
                        placeholder="Search games by title or publisher"
                        autoFocus
                    />
                </Search>
                <ControlNotice>Press Ctrl + S to open this from anywhere.</ControlNotice>
                <Button
                    onClick={() => openFullSearchPage()}
                    startIcon={<OpenInFullIcon/>}
                    sx={{mt: 1, color: 'white', borderColor: 'rgba(255,255,255,0.3)'}}
                    variant="outlined"
                >
                    Open full search page
                </Button>

                {message && (
                    <Typography sx={{color: 'white', mt: 2, fontWeight: 700}}>
                        {message}
                    </Typography>
                )}

                {searchResults.length > 0 && (
                    <List
                        sx={{
                            backgroundColor: 'rgba(255,255,255,0.92)',
                            borderRadius: '1.2em',
                            maxHeight: '70vh',
                            overflowY: 'auto',
                            marginTop: '1em',
                            padding: 1
                        }}
                    >
                        {searchResults.map(result => (
                            <Link
                                to={`/games/${result.id}`}
                                key={result.id}
                                onClick={closeAndReset}
                                style={{textDecoration: 'none', color: "black"}}
                            >
                                <HoveredListItem>
                                    <ListItemAvatar>
                                        <Avatar src={result.imageUrl} alt={result.name}/>
                                    </ListItemAvatar>
                                    <ListItemText
                                        primary={<Typography fontWeight="bolder">{result.name}</Typography>}
                                        secondary={formatSecondaryText(result)}
                                    />
                                </HoveredListItem>
                            </Link>
                        ))}
                    </List>
                )}
            </Container>
        </Modal>
    );
};

export default React.memo(QuickSearchPage);
