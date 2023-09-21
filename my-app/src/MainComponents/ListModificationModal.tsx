import React, {useEffect, useState} from 'react'
import {ModalMainPropsWithIdAndUpdate} from "../GeneralComponents/types/ModalMainProps";
import PopupMessage, {Severity} from "../GeneralComponents/PopupMessage";
import {GameStatusTypes} from "../GeneralComponents/types/GameStatusTypes";
import {
    Button,
    FormControl,
    Grid,
    InputBase,
    MenuItem,
    Modal,
    Select,
    styled,
    TextField,
    Typography
} from "@mui/material";
import axios from "axios";
import {WishlistType} from "../GeneralComponents/types/WishlistType";
import DeleteIcon from "@mui/icons-material/Delete";
import SendIcon from '@mui/icons-material/Send';


const StyledInputBase = styled(InputBase)(({theme}) => ({
    color: 'gray',
    '& .MuiInputBase-input': {
        padding: theme.spacing(2, 1, 2, 0),
        // vertical padding + font size from searchIcon
        paddingLeft: `calc(1em + ${theme.spacing(4)})`,
        transition: theme.transitions.create('width'),
        width: '100%',
        [theme.breakpoints.up('md')]: {
            width: '20ch',
        },
    },
}));

const OverlayTitle = styled(Typography)`
  position: absolute;
  bottom: 10px;
  left: 10px;
  color: white; // or any desired color
  //background-color: rgba(0, 0, 0, 0.5); // Optional: adds a dark background for better visibility
  padding: 8px;
  border-radius: 4px;
`;


const OverlayImage = styled('img')`
  position: absolute;
  top: 10px;
  right: 10px;
  width: 50px; // adjust the size as needed
  height: 50px;
`;
const BannerImage = styled('div')`
  position: relative;
  width: 100%;
  height: 200px;
  margin-bottom: 20px;
  border-top-left-radius: 1em;
  border-top-right-radius: 1em;
`;

const BackgroundImage = styled('div')<{ imageUrl: string }>`
  width: 100%;
  height: 100%;
  background-image: url(${props => props.imageUrl});
  background-size: cover;
  background-position: center;
  filter: blur(3px);
  position: absolute; // It covers the entire BannerImage
  border-top-left-radius: 1em;
  border-top-right-radius: 1em;
`;

const BlackOverlay = styled('div')`
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5); // Semi-transparent black
  border-top-left-radius: 1em;
  border-top-right-radius: 1em;
`;

const Row = styled('div')`
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
`;

const commonMargin = {
    marginX: '3em'
}

const commonPadding = {
    marginX: '3em',
    minWidth: '100%',
};

const OverlayButton = styled(Button)`
  position: absolute;
  bottom: -20px;
  right: 10px;
`;

const ListModificationModal: React.FC<ModalMainPropsWithIdAndUpdate> = ({
                                                                            open,
                                                                            onClose,
                                                                            gameId,
                                                                            updateGameInWishlist
                                                                        }) => {
    const [errorMessage, setErrorMessage] = useState('' as any);
    const [errorSeverity, setErrorSeverity] = useState<Severity>(Severity.SUCCESS);
    const [rating, setRating] = useState<number | string>("");
    const [status, setStatus] = useState<GameStatusTypes>(GameStatusTypes.PLAN_TO_PLAY);
    const [gameData, setGameData] = useState<WishlistType | null>(null)

    useEffect(() => {
        axios.get(`/api/auth/games/wishlist/${gameId}`)
            .then(response => {
                setGameData(response.data);
                if (response.data.status) {
                    setStatus(response.data.status);
                }

                if (response.data.rating) {
                    setRating(response.data.rating);
                }
            })
            .catch(error => {
                console.log(error);
                setErrorMessage(error?.response?.data?.message || error.message);
                setErrorSeverity(Severity.ERROR);
            })
    }, []);

    const handleRatingChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        let newValue = event.target.value;

        if (newValue === "") {
            setRating(newValue);
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
            setRating(String(numValue));
        } else {
            setErrorMessage('Please enter a number between 0 and 100')
        }
    }

    const handleSubmit = () => {
        axios.post('/api/auth/wishlist', {rating, status, gameId})
            .then(() => {
                updateGameInWishlist(gameId, true);

                onClose();
            }).catch(error => {
            setErrorMessage(error?.response?.data?.message || error.message);
            setErrorSeverity(Severity.ERROR);
        });
    };

    const handleDeleteClick = () => {
        axios.delete(`/api/auth/wishlist/${gameId}`).then(
            () => {
                onClose();
            }
        ).catch(error => {
                setErrorMessage(error?.response?.data?.message || error.message);
                setErrorSeverity(Severity.ERROR);
            }
        )
    }
    return (
        <Modal open={open}
               onClose={onClose}
               aria-labelledby="list-mod-modal"
               sx={{
                   display: 'flex',
                   alignItems: 'center',
                   justifyContent: 'center',
                   backdropFilter: 'blur(5px)',
               }}>
            <Grid container spacing={0}
                  sx={{
                      width: '50vw',
                      backgroundColor: 'white',
                      borderRadius: '1em',
                      boxShadow: '0 0 10px 5px rgba(255, 255, 255, 0.68)'

                  }}>
                {/* Banner Image */}
                <Grid item xs={12}>
                    <BannerImage>
                        <BackgroundImage imageUrl={gameData?.gameinfo.imageUrl || ''}/>
                        <BlackOverlay/>
                        <OverlayTitle variant="h4" fontWeight="bolder">
                            {gameData?.gameinfo.name}
                        </OverlayTitle>
                        <OverlayButton onClick={handleSubmit} sx={{marginTop: '1em', marginBottom: '2em'}}
                                       variant="contained"
                                       color="secondary"
                                       endIcon={<SendIcon/>}
                        >
                            {gameData?.gameinfo.inWishlist ? 'Update' : 'Add'}
                        </OverlayButton>
                        <OverlayImage src={gameData?.gameinfo.imageUrl || ''} alt="Game"/>
                    </BannerImage>

                    {/*</Typography>*/}
                </Grid>

                {/* Rating Input */}
                <Grid item xs={12} sx={{
                    paddingX: '3em',
                    marginBottom: '1em'
                }}>
                    <PopupMessage message={errorMessage} severity={errorSeverity}/>
                    <Typography variant="body2" fontWeight="bolder" sx={{marginY: '1em'}}>
                        Rating
                    </Typography>
                    <TextField
                        value={rating}
                        onChange={handleRatingChange}
                        autoComplete='true'
                        placeholder=''
                        type='number'
                        label="Rating"
                        variant="outlined"
                        fullWidth
                    />
                </Grid>

                {/* Status Input and Submit Button */}
                <Grid item xs={12} sx={{
                    paddingX: '3em',
                    marginBottom: '4em'
                }}>
                    <FormControl variant="outlined" fullWidth>
                        <Typography variant="body2" fontWeight="bolder" sx={{marginBottom: '1em'}}>
                            Status
                        </Typography>
                        <Select
                            value={status}
                            onChange={(event) => setStatus(event.target.value as GameStatusTypes)}
                            fullWidth
                        >
                            {Object.entries(GameStatusTypes).map(([statusKey, statusValue]) => (
                                <MenuItem key={statusValue} value={statusValue}>
                                    {statusValue}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                    {gameData?.gameinfo.inWishlist &&
                        <Button onClick={handleDeleteClick} sx={{marginTop: '2em'}} variant="contained"
                                color="primary" startIcon={<DeleteIcon/>}>
                            Delete
                        </Button>
                    }

                </Grid>
            </Grid>
        </Modal>
    )
}
export default ListModificationModal