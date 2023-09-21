import React, {useEffect, useRef, useState} from 'react';
import '../App.scss';
import axios from "axios";
import {
    Box,
    Card,
    CardContent,
    CardMedia,
    CircularProgress,
    Container,
    Grid,
    IconButton,
    Link as MuiLink,
    Tooltip,
    Typography
} from "@mui/material";

import AddIcon from '@mui/icons-material/Add';
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {GameData, GameDataWithInteractions} from "../GeneralComponents/types/GameData";
import {useLoading} from "../GeneralComponents/LoadingContext";
import ListModificationModal from "./ListModificationModal";
import EditIcon from '@mui/icons-material/Edit';
import EventNoteIcon from '@mui/icons-material/EventNote';
import DeleteIcon from "@mui/icons-material/Delete";

function HomePage() {

    const [games, setGames] = useState<GameDataWithInteractions[]>([]);
    const [alertMessage, setAlertMessage] = useState('');
    const [severity, setSeverity] = React.useState(Severity.SUCCESS);
    const [page, setPage] = useState(1);
    const lastGameInfo = useRef({lastId: null, lastInteractions: null});

    const observerRef = useRef(null);
    const lastObservedRef = useRef(null);

    const [GameEntryModalOpen, setGameEntryModalOpen] = React.useState(false);
    const [modalGameId, setModalGameId] = React.useState('' as any);


    const loadingContext = useLoading()

    if (!loadingContext) {
        throw new Error("useLoading must be used within a LoadingProvider");
    }

    const updateGameInWishlist = (gameId: string, inWishlist: boolean) => {
        const updatedGames = games.map((game: GameDataWithInteractions) => {
            if (game.id === gameId) {
                game.inWishlist = inWishlist;
            }
            return game;
        });
        setGames(updatedGames);
    }


    const {loading, setLoading} = loadingContext;


    const gameLimit: number = 40;

    useEffect(() => {
        const observer = new IntersectionObserver(
            (entries) => {
                console.log("Observer callback called");

                if (entries[0].isIntersecting) {
                    setPage((prevPage) => prevPage + 1);
                }
            },
            {threshold: .5}
        );

        if (observerRef.current) {
            if (lastObservedRef.current) {
                observer.unobserve(lastObservedRef.current);
            }
            observer.observe(observerRef.current);
        }

        lastObservedRef.current = observerRef.current;

        return () => {
            if (observerRef.current) {
                observer.unobserve(observerRef.current);
            }
        };
    }, [games]);

    useEffect(() => {
        const {lastId, lastInteractions} = lastGameInfo.current;
        console.log(lastId, lastInteractions)
        let endpoint = `/api/auth/games/most-interacted-with?limit=${gameLimit}`;

        if (lastId && lastInteractions) {
            endpoint += `&lastId=${lastId}&lastInteractions=${lastInteractions}`;
        }

        setLoading(true);
        axios.get(endpoint)
            .then(response => {
                const newGames = response?.data || [];

                setGames(prevGames => [...prevGames, ...newGames]);

                // Update the reference values with the last game's info
                if (newGames.length > 0) {
                    const lastGame = newGames[newGames.length - 1];
                    lastGameInfo.current = {lastId: lastGame.id, lastInteractions: lastGame.interactions};
                }

                setLoading(false);
            })
            .catch(error => {
                setAlertMessage(error?.response?.data?.message || error.message);
            });
    }, [page]);

    const handleOpenGameEntryModal = (gameId: string) => {
        setModalGameId(gameId);
        setGameEntryModalOpen(true);
    }
    const handleCloseGameEntryModal = () => {
        setModalGameId('');
        setGameEntryModalOpen(false);
    }


    const upsertWishlist = (game: GameData) => {
        axios.post("/api/auth/wishlist", {
            gameId: game.id,
            status: "Plan to Play",
            rating: null
        })
            .then(response => {
                setAlertMessage(response?.data?.message || 'Game added to wishlist');
                setSeverity(Severity.SUCCESS);

                // Update the local state
                game.inWishlist = true;
                setGames(prevGames => [...prevGames]);
            }).catch(error => {
            setAlertMessage(error?.response?.data?.message || error.message);
            setSeverity(Severity.ERROR);
        })
    }

    const removeFromWishlist = (game: GameData) => {
        axios.delete(`/api/auth/wishlist/${game.id}`)
            .then(response => {
                setAlertMessage(response?.data?.message || 'Game removed from wishlist');
                setSeverity(Severity.SUCCESS);

                // Update the local state
                game.inWishlist = false;
                setGames(prevGames => [...prevGames]);
            }).catch(error => {
            setAlertMessage(error?.response?.data?.message || error.message);
            setSeverity(Severity.ERROR);
        })
    }

    const toggleWishlist = (game: GameData) => {
        game.inWishlist ? removeFromWishlist(game) : upsertWishlist(game);

    };


    // Icon Component
    const WishlistIcon = ({gameId, inWishlist}: { gameId: string, inWishlist: boolean }) => {
        return (
            <Tooltip title={inWishlist ? "Edit Wishlist Entry" : "Add to Wishlist"} placement="left">
                <IconButton
                    color="primary"
                    onClick={() => handleOpenGameEntryModal(gameId)}
                    sx={{
                        color: "white",
                    }}
                    className="showOnHover"
                >
                    {inWishlist ? <EditIcon fontSize="medium"/> : <AddIcon fontSize="medium"/>}
                </IconButton>
            </Tooltip>
        );
    };

    const QuickActionIcon = ({gameData, inWishlist}: { gameData: GameData, inWishlist: boolean }) => {
        return (
            <Tooltip title={inWishlist ? "Set to Planning" : "Add to Planning"} placement="left">
                <IconButton
                    color="primary"
                    onClick={() => upsertWishlist(gameData)}
                    sx={{
                        color: "white",
                    }
                    }
                    className="quickActionIcon"
                >
                    <EventNoteIcon fontSize="medium"/>
                </IconButton>
            </Tooltip>
        )
    }
    const DeleteIconButton = ({gameData}: { gameData: GameData }) => {
        return (
            <Tooltip title="Remove from Wishlist" placement="left">
                <IconButton
                    color="primary"
                    onClick={() => removeFromWishlist(gameData)}
                    sx={{
                        color: "white",
                    }}
                    className="quickActionIcon"
                >
                    <DeleteIcon fontSize="medium"/>
                </IconButton>
            </Tooltip>
        );
    }

    return (
        <div>
            <Container>
                <Typography
                    variant="h3"
                    component="div"
                    gutterBottom
                    style={{textAlign: "center"}}
                    fontWeight="bolder"
                >
                    Most Popular Games
                </Typography>
                <PopupMessage message={alertMessage} severity={severity}/>
                <Grid container spacing={3} sx={{height: '78vh'}}>
                    {games.map((game: GameData, index) => (
                        <Grid item xs={12} sm={6} md={4} lg={3} key={game.id}
                              ref={index === games.length - 1 ? observerRef : null}
                        >

                            <Card className="gameCards"
                                  sx={{
                                      borderRadius: '1em',
                                      backgroundColor: 'rgba(255, 255, 255, 0.6)', // white with 60% opacity
                                      position: 'relative', // Add this line
                                      // '&:hover .showOnHover': { // New hover effect for the CardActions
                                      //     opacity: 1,
                                      //     backgroundColor: "rgba(0,0,0,.7)",
                                      // },
                                      // '&:hover': {
                                      //     boxShadow: '0px 0px 15px 5px rgba(255, 255, 255, 0.6)',  // Adjust as needed
                                      // }
                                  }}>
                                <Box className="iconsContainer">
                                    <WishlistIcon gameId={game.id} inWishlist={game.inWishlist}/>
                                    <QuickActionIcon gameData={game} inWishlist={game.inWishlist}/>
                                    {game.inWishlist && <DeleteIconButton gameData={game}/>}
                                </Box>

                                <MuiLink href={"/api/games/" + game.id} color="inherit" underline="hover">
                                    <CardMedia
                                        component="img"
                                        image={game.imageUrl}
                                        alt={game.name}
                                        style={{
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            flexDirection: 'column',
                                            // boxShadow: '0 0 10px 10px rgba(255, 255, 255, 0.68)'
                                        }}
                                    />
                                </MuiLink>
                                <CardContent>
                                    <Typography gutterBottom variant="body2" component="div" fontWeight="bolder">
                                        {game.name}
                                    </Typography>

                                </CardContent>

                            </Card>
                        </Grid>
                    ))}
                    {loading && <CircularProgress/>}


                    {modalGameId &&
                        <ListModificationModal open={GameEntryModalOpen} onClose={handleCloseGameEntryModal}
                                               gameId={modalGameId} updateGameInWishlist={updateGameInWishlist}/>
                    }

                </Grid>
            </Container>
        </div>
    );
}

export default HomePage;