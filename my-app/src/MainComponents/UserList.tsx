import React, {useEffect} from 'react';
import {
    Avatar,
    Box,
    Container,
    IconButton,
    Input,
    List,
    ListItem,
    ListItemAvatar,
    ListItemText,
    MenuItem,
    Paper,
    Select,
    SelectChangeEvent,
    Typography
} from '@mui/material';
import StarIcon from '@mui/icons-material/Star';
import DeleteIcon from '@mui/icons-material/Delete';

import axios from "axios";
import {Link} from "react-router-dom";
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {WishlistType} from "../GeneralComponents/types/WishlistType";
import {GameStatusTypes} from "../GeneralComponents/types/GameStatusTypes";

const GameList = () => {

    const [gameList, setGameList] = React.useState<WishlistType[]>([] as any);
    const [alertMessage, setAlertMessage] = React.useState('');

    const [editingGameId, setEditingGameId] = React.useState<string | null>(null);
    const [editingGameRating, setEditingGameRating] = React.useState<number | string | null>(null);
    const [severity, setSeverity] = React.useState(Severity.SUCCESS);

    const [editingStatusId, setEditingStatusId] = React.useState<string | null>(null);
    const [editingGameStatus, setEditingGameStatus] = React.useState<GameStatusTypes>(GameStatusTypes.PLAN_TO_PLAY)


    useEffect(() => {
        axios.get('/api/auth/games/wishlist')
            .then(response => {
                setGameList(response?.data || []);

            }).catch(error => {
            setAlertMessage(error?.response?.data?.message || error.message);

        })
    }, []);
    // Sample data for games with image URLs and ratings

    const handleRatingEdit = (gameId: string, currentRating: number | string) => {
        setEditingGameId(gameId);
        setEditingGameRating(currentRating);
    }

    const handleStatusEdit = (gameId: string, currentStatus: GameStatusTypes) => {
        setEditingStatusId(gameId);
        setEditingGameStatus(currentStatus);

    }
    const handleRatingChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        let newValue = event.target.value;

        if (newValue === "") {
            setEditingGameRating(newValue);
            return;
        }
        if (!isNaN(Number(newValue))) {
            let numValue = Number(newValue);

            // Clamp the value between 0 and 100
            if (numValue < 0) {
                numValue = 0;
            } else if (numValue > 100) {
                numValue = 100;
            }

            // Update state with the clamped value
            setEditingGameRating(String(numValue));
        } else {
            setAlertMessage('Please enter a number between 0 and 100')
        }
    }

    const handleStatusChange = (e: SelectChangeEvent<GameStatusTypes>) => {
        setEditingGameStatus(e.target.value as GameStatusTypes)
    }

    const removeFromList = (gameId: string) => {
        axios.delete(`/api/auth/wishlist/${gameId}`)
            .then(response => {
                setAlertMessage(response?.data?.message || 'Game removed from list');
                setGameList((prevGames: any[]) => prevGames.filter((game: any) => game.gameinfo.id !== gameId));
            }).catch(error => {
            setAlertMessage(error?.response?.data?.message || error.message);

        })
    }

    const handleRatingSave = (gameId: string) => {
        axios.post(`/api/auth/games/${gameId}/rating`, {rating: editingGameRating})
            .then(response => {
                setGameList((prevGames: any[]) =>
                    prevGames.map(game => game.gameinfo.id === gameId
                        ? {...game, rating: editingGameRating}
                        : game)
                );
                setEditingGameId(null);
            });
    }

    const handleStatusSave = (gameId: string) => {
        axios.put(`/api/auth/games/${gameId}/status/${editingGameStatus}`)
            .then(response => {
                setGameList((prevGames: WishlistType[]) =>
                    prevGames.map(game => game.gameinfo.id === gameId
                        ? {...game, status: editingGameStatus}
                        : game)
                );
                setEditingStatusId(null);
            })
    }

    function getOrganizedGamesByStatus(gameList: WishlistType[]): { [status in GameStatusTypes]?: WishlistType[] } {
        return gameList.reduce((acc, game) => {
            const status = game.status;
            if (!acc[status]) {
                acc[status] = [];
            }
            (acc[status as GameStatusTypes] || []).push(game);
            return acc;
        }, {} as { [status in GameStatusTypes]?: WishlistType[] });
    }

    return (
        <Container sx={{height: '78vh'}}>
            <Typography variant="h4" fontWeight="bolder" gutterBottom>
                My Game List
            </Typography>
            <PopupMessage message={alertMessage} severity={severity}/>
            <Paper elevation={0} sx={{backgroundColor: 'transparent'}}>
                {Object.entries(getOrganizedGamesByStatus(gameList)).map(([status, games]) => (
                    <Box key={status} id={status}>
                        <Typography variant="h5" fontWeight="bolder">{status}</Typography>
                        <List>
                            {games.map((game: WishlistType) => (
                                <ListItem key={game.id}
                                          secondaryAction={
                                              <IconButton edge="end" aria-label="add" onClick={() => {
                                                  removeFromList(game.gameinfo.id)
                                              }}>
                                                  <DeleteIcon/>
                                              </IconButton>

                                          }
                                          sx={{
                                              backgroundColor: 'rgba(255, 255, 255, 0.6)', // white with 60% opacity
                                              borderRadius: '1em',
                                              marginBottom: '1em', // Optional: Add some margin to separate the items for better visuals
                                              transition: 'boxShadow 0.3s',  // Smooth transition effect for boxShadow
                                              '&:hover': {
                                                  boxShadow: '0px 0px 15px 5px rgba(0, 0, 0, 0.6)',  // Adjust as needed
                                              }
                                          }}
                                >
                                    <ListItemAvatar>
                                        <Avatar src={game.gameinfo.imageUrl} alt={game.gameinfo.name}/>
                                    </ListItemAvatar>
                                    <ListItemText
                                        primary={
                                            <Link
                                                to={`/games/${game.gameinfo.id}`}  // Adjust this according to your route setup
                                                style={{textDecoration: 'none', color: 'inherit'}}
                                            >
                                                {game.gameinfo.name}
                                            </Link>}
                                        secondary={
                                            <Box component={"span"} display="flex" alignItems="center">
                                                <StarIcon color="primary" fontSize="small"/>
                                                {editingGameId === game.gameinfo.id ? (
                                                    <Box component="span" display="flex" alignItems="center">
                                                        <Input
                                                            value={editingGameRating}
                                                            onChange={handleRatingChange}
                                                            onBlur={() => handleRatingSave(game.gameinfo.id)}
                                                            autoFocus
                                                            type="text" // Allow text so the user can clear the input

                                                            sx={{ml: 1, width: '50px'}}
                                                            inputProps={{
                                                                pattern: "^\d*(\.\d{0,1})?$",  // Numbers with up to one decimal place
                                                                maxLength: 3  // At most 3 characters for numbers between 0 and 100
                                                            }}
                                                        />
                                                    </Box>
                                                ) : (
                                                    <Typography
                                                        variant="body2"
                                                        sx={{ml: 1, cursor: 'pointer'}}
                                                        onClick={() => handleRatingEdit(game.gameinfo.id, game.rating)}
                                                    >
                                                        {game.rating || "_"}
                                                    </Typography>
                                                )}
                                            </Box>
                                        }
                                    />

                                    <Box
                                        sx={{
                                            // flexGrow: 1,
                                            display: 'flex',
                                            justifyContent: 'center',
                                            alignItems: 'center',
                                            cursor: 'pointer'
                                        }}
                                    >
                                        {editingStatusId === game.gameinfo.id
                                            ? (
                                                <Select
                                                    value={editingGameStatus}
                                                    onChange={handleStatusChange}
                                                    onBlur={() => handleStatusSave(game.gameinfo.id)}
                                                    autoFocus
                                                    sx={{
                                                        maxHeight: '20vh',
                                                        overflowY: 'auto'
                                                    }}
                                                >
                                                    {Object.entries(GameStatusTypes).map(([statusKey, statusValue]) => (
                                                        <MenuItem key={statusValue} value={statusValue} sx={{
                                                            fontWeight: 'bolder'
                                                        }}>
                                                            {statusValue}
                                                        </MenuItem>
                                                    ))}
                                                </Select>
                                            )

                                            : (
                                                <Typography variant="body2" fontWeight="bolder"
                                                            onClick={() => handleStatusEdit(game.gameinfo.id, game.status)}>
                                                    {game.status || "_"} {/* Replace with your game's status property */}
                                                </Typography>
                                            )
                                        }
                                    </Box>

                                </ListItem>
                            ))}
                        </List>
                    </Box>
                ))}

            </Paper>
            <Box sx={{
                position: 'fixed',
                top: '20%',
                left: 0,
                width: 'auto',
                maxHeight: '60vh',
                overflowY: 'auto',
                marginLeft: '3em',
                backgroundColor: 'rgba(255,255,255,0.68)',
                borderRadius: '1em'
            }}>
                {Object.values(GameStatusTypes).map(status => (
                    <Typography
                        key={status}
                        fontWeight="bolder"
                        variant="body2"
                        sx={{cursor: 'pointer', padding: 2}}
                        onClick={() => {
                            const section = document.getElementById(status);
                            section?.scrollIntoView({behavior: 'smooth'});
                        }}
                    >
                        {status}
                    </Typography>
                ))}
            </Box>
        </Container>
    );
};

export default GameList;
