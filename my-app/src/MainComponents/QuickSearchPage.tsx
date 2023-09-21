import {
    alpha,
    Avatar,
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
import axios from "axios";
import {Link} from "react-router-dom";
import {GameData} from "../GeneralComponents/types/GameData";
import {ModalMainProps} from "../GeneralComponents/types/ModalMainProps";


const Search = styled('div')(({theme}) => ({
    position: 'relative',
    borderRadius: '1em',
    backgroundColor: alpha(theme.palette.common.white, 0.7),
    '&:hover': {
        backgroundColor: alpha(theme.palette.common.white, 0.9),
    },
    marginRight: theme.spacing(2),
    marginLeft: 0,
    width: '100%',
    [theme.breakpoints.up('sm')]: {
        marginLeft: theme.spacing(3),
        width: 'auto',
    },
}));
const SearchIconWrapper = styled('div')(({theme}) => ({
    padding: theme.spacing(0, 2),
    height: '100%',
    position: 'absolute',
    pointerEvents: 'none',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
}));
const StyledInputBase = styled(InputBase)(({theme}) => ({
    color: 'inherit',
    '& .MuiInputBase-input': {
        padding: theme.spacing(2, 1, 2, 0),
        // vertical padding + font size from searchIcon
        paddingLeft: `calc(1em + ${theme.spacing(4)})`,
        transition: theme.transitions.create('width'),
        width: '100%',
        [theme.breakpoints.up('md')]: {
            width: '20ch',
        },
        '&::placeholder': {
            color: 'rgb(105,105,105)',  // For example, making the placeholder text red
            fontStyle: 'italic',  // Making it italic
            opacity: 1,  // To ensure full color opacity
            fontWeight: 'bolder'
        }
    },
}));
const HoveredListItem = styled(ListItem)(({theme}) => ({
    '&:hover': {
        backgroundColor: alpha(theme.palette.primary.main, 0.1), // Change the background color on hover
        boxShadow: '0px 0px 10px 2px rgba(0, 0, 0, 0.1)', // Add a glow/shadow effect
    },
}));

const ControlNotice = styled(Typography)(({theme}) => ({
    color: 'white', // Subtle color
    textAlign: 'right',
    fontSize: '0.8rem', // Make it smaller than standard text
    marginTop: theme.spacing(1), // Add some top margin
    fontWeight: 'bolder'
}));
const QuickSearchPage: React.FC<ModalMainProps> = ({open, onClose}) => {
    const [searchQuery, setSearchQuery] = useState('');
    const [errorMessage, setErrorMessage] = useState('');
    const [searchResults, setSearchResults] = useState<GameData[]>([]);
    const searchLimit: number = 40;

    const searchBarRef = React.useRef<HTMLDivElement | null>(null);


    const debouncedSearch = React.useCallback(
        debounce((query: string) => {
            axios.get(`/api/auth/games/search?query=${query}&limit=${searchLimit}`)
                .then(response => {
                    setSearchResults(response.data);
                })
                .catch(error => {
                    console.log(error)
                    // setErrorMessage((error.response && error.response.data && error.response.data.message) || error.message);
                    setSearchResults([])
                });
        }, 300), []);

    useEffect(() => {
        if (searchQuery) {
            debouncedSearch(searchQuery);

        } else {
            setSearchResults([]);
        }

        return () => debouncedSearch.clear();

    }, [searchQuery, debouncedSearch]);

    const onLinkClick = (id: string) => {
        onClose();
        setSearchQuery(''); // Clear search query
    }

    const onCloseClearOut = () => {
        onClose();
        setSearchQuery("")
    }

    return (
        <Modal open={open}
               onClose={() => onCloseClearOut()}
               aria-labelledby="search-modal"
               sx={{
                   display: 'flex',
                   alignItems: 'flex-start',
                   justifyContent: 'center',
                   backdropFilter: 'blur(5px)',
                   padding: '10vh'
               }}
        >

            <Container>
                {/*<PopupMessage message={errorMessage} severity={Severity.SUCCESS}/>*/}
                <Search ref={searchBarRef}>
                    <SearchIconWrapper>
                        <SearchIcon/>
                    </SearchIconWrapper>
                    <StyledInputBase
                        fullWidth
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder={"Search Games"}
                        type={'text'}
                        autoComplete='true'
                    />
                </Search>
                <ControlNotice>Hint: Can Press "Control + S" to pull up quick search</ControlNotice>
                {searchResults?.length > 0 && (
                    <List style={{
                        // top: searchBarRef.current ? searchBarRef.current.offsetTop + searchBarRef.current.offsetHeight + 'px' : '100%',
                        // left: searchBarRef.current ? searchBarRef.current.offsetLeft + 'px' : '0',
                        backgroundColor: 'rgba(255,255,255,0.8)',
                        borderRadius: '1em',
                        // width: searchBarRef.current ? searchBarRef.current.offsetWidth + 'px' : 'auto',
                        maxHeight: '70vh',
                        overflowY: 'auto',
                        marginTop: '1em', // Provide a bit of spacing between the search bar and the list


                    }}>
                        {searchResults.map((result: any) => (
                            <Link to={`/games/${result.id}`} key={result.id} onClick={() => onLinkClick(result.id)}
                                  style={{textDecoration: 'none', color: "black"}}>

                                <HoveredListItem sx={{marginBotton: '1em'}}
                                                 onClick={() => onLinkClick(result.id)}>
                                    <ListItemAvatar>
                                        <Avatar src={result.imageUrl} alt={result.name}/>
                                    </ListItemAvatar>
                                    <ListItemText
                                        primary={
                                            <Typography fontWeight="bolder">
                                                {result.name}
                                            </Typography>
                                        }
                                    />
                                </HoveredListItem>
                            </Link>
                        ))}

                    </List>)

                }

            </Container>


        </Modal>

    )
}

export default React.memo(QuickSearchPage);