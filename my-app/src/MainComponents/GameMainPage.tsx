import React, {useEffect, useState} from 'react';
import {useParams} from "react-router-dom";
import axios from "axios";
import {CircularProgress, Typography} from "@mui/material";
import PictureBanner from "../GeneralComponents/PictureBanner";
import '../App.scss';

function GameMainPage() {
    const {gameId} = useParams();
    const [game, setGame] = useState({} as any);
    const [errorMessage, setErrorMessage] = useState('' as any);

    useEffect(() => {
        axios.get('/api/auth/games/' + gameId)
            .then(response => {
                console.log(response?.data)
                setGame(response?.data || {});
            }).catch(error => {
            setErrorMessage(error?.response?.data?.message || error.message);
            console.log(error);
        })
    }, [gameId])

    if (!game || game.isEmpty) {
        return <CircularProgress/>
    }

    return (
        <div>
            <PictureBanner
                imageUrl={game.imageUrl}
                imageAlt={game.name}
            />
            {/*<div className="gamepage-mainpic">*/}
            {/*    <Grid item xs={12} md={5}>*/}

            {/*        <CardMedia*/}
            {/*            component="img"*/}
            {/*            image={game.imageUrl}*/}
            {/*            alt={game.name}*/}
            {/*        />*/}
            {/*    </Grid>*/}

            {/*</div>*/}
            <Typography
                variant="h3"
                component="div"
                gutterBottom
                style={{textAlign: "center"}}
                fontWeight="bolder"
            >
                {game.name}
            </Typography>
        </div>
    );
}

export default GameMainPage;
