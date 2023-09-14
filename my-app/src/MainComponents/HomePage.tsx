import React, {useEffect, useState} from 'react';
import '../App.scss';
import axios from "axios";
import {
    Card,
    CardContent,
    CardMedia,
    Container,
    Grid,
    IconButton,
    Link as MuiLink,
    Tooltip,
    Typography
} from "@mui/material";

import AddIcon from '@mui/icons-material/Add';
import PopupMessage from "../GeneralComponents/PopupMessage";


function HomePage() {

    const [games, setGames] = useState([]);
    const [alertMessage, setAlertMessage] = useState('');

    const gameLimit: number = 40;

    useEffect(() => {
        axios.get('/api/auth/randomGameData?limit=' + gameLimit)
            .then(response => {
                setGames(response?.data || []);

            }).catch(error => {
            setAlertMessage(error?.response?.data?.message || error.message);

        })
    }, [])

    const addToWishlist = (gameId: string) => {
        axios.post('/api/auth/addToList', {gameId})
            .then(response => {
                setAlertMessage(response?.data?.message || 'Game added to wishlist');

            }).catch(error => {
            setAlertMessage(error?.response?.data?.message || error.message);

        })
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
                    Featured Games
                </Typography>
                <PopupMessage message={alertMessage}/>
                <Grid container spacing={3} sx={{height: '78vh'}}>
                    {games.map((game: any) => (
                        <Grid item xs={12} sm={6} md={4} lg={3} key={game.id}>
                            <Card className="gameCards" sx={{
                                borderRadius: '1em',
                                backgroundColor: 'transparent',
                                position: 'relative', // Add this line
                                '&:hover .showOnHover': { // New hover effect for the CardActions
                                    opacity: 1
                                }
                                // boxShadow: '0 0 10px 10px rgba(255, 255, 255, 0.68)'
                            }}>

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

                                    <Tooltip title="Add to Wishlist">
                                        <IconButton color="primary" onClick={() => addToWishlist(game.id)}
                                                    sx={{
                                                        opacity: 0,
                                                        transition: 'opacity 0.3s ease-in-out',
                                                        position: 'absolute',  // <-- Position it absolute
                                                        top: 10,  // <-- Position from the top
                                                        right: 10,  // <-- Position from the right
                                                        zIndex: 10
                                                    }}
                                                    className="showOnHover">
                                            <AddIcon fontSize="large" fontWeight="bolder"/>
                                        </IconButton>
                                    </Tooltip>
                                </CardContent>

                            </Card>
                        </Grid>
                    ))}
                </Grid>
            </Container>
        </div>
        // </ThemeProvider>
    );
}

export default HomePage;