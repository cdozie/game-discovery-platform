import React, {useEffect, useState} from 'react';
import {useParams} from "react-router-dom";
import axios from "axios";
import {CircularProgress, Typography} from "@mui/material";
import PictureBanner from "../GeneralComponents/PictureBanner";
import '../App.scss';
import {GameData} from "../GeneralComponents/types/GameData";
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";

function GameMainPage() {
    const {gameId} = useParams<string>();
    const [game, setGame] = useState<GameData>({} as any);
    const [errorMessage, setErrorMessage] = useState('' as any);
    const [errorSeverity, setErrorSeverity] = useState<Severity>(Severity.SUCCESS);

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

    useEffect(() => {
        axios.post('/api/auth/games/interactions', {gameId: gameId})
            .then(r => {
                console.log("Recorded interaction")
            })
            .catch(error => {
                console.log(error)
                setErrorMessage(error?.response?.data?.message || error.message)
                setErrorSeverity(Severity.ERROR)

            })
    }, []);

    if (!game) {
        return <CircularProgress/>
    }

    return (
        <div>
            <PictureBanner
                imageUrl={game.imageUrl}
                imageAlt={game.name}
            />

            <PopupMessage message={errorMessage} severity={errorSeverity}/>
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
