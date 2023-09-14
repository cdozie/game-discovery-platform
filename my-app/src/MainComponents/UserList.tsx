import React, {useEffect} from 'react';
import {Avatar, Box, Container, List, ListItem, ListItemAvatar, ListItemText, Paper, Typography} from '@mui/material';
import StarIcon from '@mui/icons-material/Star';
import axios from "axios";

const GameList = () => {

    const [gameList, setGameList] = React.useState([] as any);
    const [alertMessage, setAlertMessage] = React.useState('');

    useEffect(() => {
        axios.get('/api/auth/games/getList')
            .then(response => {
                setGameList(response?.data || []);

            }).catch(error => {
            setAlertMessage(error?.response?.data?.message || error.message);

        })
    }, []);
    // Sample data for games with image URLs and ratings


    return (
        <Container sx={{height: '78vh'}}>
            <Typography variant="h4" fontWeight="bolder" gutterBottom>
                My Game List
            </Typography>

            <Paper elevation={3}>
                <List>
                    {gameList.map((game: any) => (
                        <ListItem key={game.id}>
                            <ListItemAvatar>
                                <Avatar src={game.gameinfo.imageUrl} alt={game.gameinfo.name}/>
                            </ListItemAvatar>
                            <ListItemText
                                primary={game.gameinfo.name}
                                secondary={
                                    <Box display="flex" alignItems="center">
                                        <StarIcon color="primary" fontSize="small"/>
                                        <Typography variant="body2" sx={{ml: 1}}>
                                            {game.rating}
                                        </Typography>
                                    </Box>
                                }
                            />
                        </ListItem>
                    ))}
                </List>
            </Paper>
        </Container>
    );
};

export default GameList;
