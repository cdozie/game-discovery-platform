import React from 'react';
import {Link, useLocation, useNavigate} from 'react-router-dom';

import '../App.scss';
import {Alert, Box, Button, Container, Typography} from '@mui/material';
import axios from "axios";
import {ThemeProvider} from "@mui/material/styles";
import overrideTheme from "../themes";
import FormFieldInput from "./FormComponents";
import PasswordField from "./PasswordField";

function LoginForm() {
    const [username, setUsername] = React.useState('');
    const [password, setPassword] = React.useState('');


    const [formMessage, setFormMessage] = React.useState('');
    const [formMessageType, setFormMessageType] = React.useState('success');
    const navigate = useNavigate();

    const location = useLocation();
    const queryParams = new URLSearchParams(location.search);
    const verificationStatus = queryParams.get('verified');
    const handleUsernameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setUsername(e.target.value);
    }


    const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setPassword(e.target.value);
    }

    const handleLogInSubmit = (event: React.FormEvent) => {
        event.preventDefault();
        axios.post("/api/auth/login", {
            username: username,
            password: password
        })
            .then(response => {
                setFormMessage(response.data.message);
                setFormMessageType('success');
                navigate('/')
            })
            .catch(error => {
                setFormMessage(error.response.data.message || error.message);
                setFormMessageType('error');
            });
    }
    // Have it so that it redirects to the home page if the user is already logged in and also have it get any messages from the backend
    React.useEffect(() => {
        if (verificationStatus === 'true') {
            setFormMessage('Your account has been verified. Please log in to continue.');
            setFormMessageType('success');
        }
    }, [verificationStatus])

    return (
        <ThemeProvider theme={overrideTheme}>
            <Container>
                <Box className="centerCard">

                    <Typography align='left' variant="h6" gutterBottom fontWeight="bolder"
                                sx={{marginBottom: '1em'}}>
                        Log In:
                    </Typography>

                    <FormFieldInput label={'Enter Username/Email'}
                                    type={'text'}
                                    value={username}
                                    onChange={handleUsernameChange}
                                    placeholder={''}
                                    autoComplete={true}
                    />

                    <PasswordField
                        label="Enter Password"
                        value={password}
                        onChange={handlePasswordChange}
                        placeholder={''}
                    />

                    {formMessage && (

                        <Alert severity={formMessageType === 'success' ? 'success' : 'error'}
                               sx={{marginBottom: '1em'}}>
                            {/*<AlertTitle>{backendResponseType.charAt(0).toUpperCase() + backendResponseType.slice(1)}</AlertTitle>*/}
                            <strong>{formMessage}</strong>
                        </Alert>
                    )}
                    <Button variant="contained" color="primary" onClick={handleLogInSubmit}
                            sx={{marginTop: '1em', marginBottom: '1em', alignSelf: 'flex-start'}}>
                        Login
                    </Button>
                    <Typography variant="body2">
                        Don't have an account? Sign up <Link to="/register">here</Link>.
                    </Typography>

                </Box>
            </Container>
        </ThemeProvider>

    );
}

export default LoginForm;
