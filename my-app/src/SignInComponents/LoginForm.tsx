import React from 'react';
import {Link, useLocation, useNavigate} from 'react-router-dom';

import '../App.scss';
import {Alert, Box, Button, Container, Typography} from '@mui/material';
import {ThemeProvider} from "@mui/material/styles";
import overrideTheme from "../themes";
import FormFieldInput from "./FormComponents";
import PasswordField from "./PasswordField";
import {getErrorMessage} from "../api/client";
import {login} from "../api/games";

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

    const handleLogInSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        try {
            await login(username, password);
            setFormMessage('Login successful.');
            setFormMessageType('success');
            navigate('/');
        } catch (error) {
            setFormMessage(getErrorMessage(error));
            setFormMessageType('error');
        }
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
                    <form onSubmit={handleLogInSubmit}>
                        <FormFieldInput label={'Enter Username/Email'}
                                        type={'text'}
                                        value={username}
                                        onChange={handleUsernameChange}
                                        placeholder={''}
                                        autoComplete="username"
                                        name="username"
                        />

                        <PasswordField
                            label="Enter Password"
                            value={password}
                            onChange={handlePasswordChange}
                            placeholder={''}
                            autoComplete="current-password"
                            name="password"
                        />

                        {formMessage && (
                            <Alert severity={formMessageType === 'success' ? 'success' : 'error'}
                                   sx={{marginBottom: '1em'}}>
                                {/*<AlertTitle>{backendResponseType.charAt(0).toUpperCase() + backendResponseType.slice(1)}</AlertTitle>*/}
                                <strong>{formMessage}</strong>
                            </Alert>
                        )}

                        <Button variant="contained" color="primary" type="submit"
                                sx={{marginTop: '1em', marginBottom: '1em', alignSelf: 'flex-start'}}>
                            Login
                        </Button>
                        <Typography variant="body2">
                            Don't have an account? Sign up <Link to="/register">here</Link>.
                        </Typography>
                    </form>
                </Box>
            </Container>
        </ThemeProvider>

    );
}

export default LoginForm;
