import React from 'react';
import Snackbar from '@mui/material/Snackbar';
import MuiAlert from '@mui/material/Alert';

interface MessageProps {
    message?: string | null;
    severity?: Severity;
}

export enum Severity {
    ERROR = "error",
    SUCCESS = "success",
    INFO = "info",
    WARNING = "warning"
}

// This function helps determine the direction of the slide
// const SlideTransition: React.FC<TransitionProps> = (props) => (
//     <Slide {...props} direction="down" children={}/>
// );

const PopupMessage: React.FC<MessageProps> = ({message, severity = Severity.SUCCESS}: MessageProps) => {
    const [open, setOpen] = React.useState<boolean>(!!message);

    const handleClose = (event: React.SyntheticEvent | Event, reason?: string) => {
        if (reason === 'clickaway') {
            return;
        }
        setOpen(false);
    };

    // useEffect(() => {
    //     setOpen(!!message);
    // }, [message]);
    return (
        <Snackbar
            open={open}
            autoHideDuration={6000}
            onClose={handleClose}
            // TransitionComponent={SlideTransition}
            anchorOrigin={{vertical: 'top', horizontal: 'center'}}
        >
            <MuiAlert onClose={handleClose} severity={severity} variant="filled">
                {message}
            </MuiAlert>
        </Snackbar>

    );
}

export default PopupMessage;
