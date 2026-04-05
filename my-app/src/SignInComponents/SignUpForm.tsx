import React from 'react';

import '../App.scss';
import {ThemeProvider} from "@mui/material/styles";
import overrideTheme from "../themes";
import {Alert, Box, Button, Container, Link as MuiLink, Stack, Typography} from "@mui/material";
import {Link} from "react-router-dom";
import FormFieldInput from "./FormComponents";
import PasswordField from "./PasswordField";
import {getErrorMessage} from "../api/client";
import {register, VerificationResponse} from "../api/games";

function SignUpForm() {
    const [username, setUsername] = React.useState('');
    const [password, setPassword] = React.useState('');
    const [email, setEmail] = React.useState('');
    const [formMessage, setFormMessage] = React.useState('');
    const [formMessageType, setFormMessageType] = React.useState('success');
    const [verificationInfo, setVerificationInfo] = React.useState<VerificationResponse | null>(null);

    const handleUsernameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFormMessage('');
        setVerificationInfo(null);
        setUsername(e.target.value);
    }
    const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFormMessage('');
        setVerificationInfo(null);
        setPassword(e.target.value);
    }
    const handleEmailChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const emailInput = e.target.value;
        setEmail(emailInput);
        setVerificationInfo(null);
        if (isValidEmail(emailInput)) {
            setFormMessage(''); // clear error message if email is valid
        } else {
            setFormMessage('Invalid email address');
            setFormMessageType('error');

        }


    }

    const handleSignUpSubmit = async (event: React.FormEvent) => {
        event.preventDefault();
        try {
            const response = await register(username, email, password);
            setVerificationInfo(response);
            setFormMessage(response.message || '');
            setFormMessageType('success');
        } catch (error) {
            setVerificationInfo(null);
            setFormMessage(getErrorMessage(error));
            setFormMessageType('error');
        }
    }

    function isValidEmail(email: string): boolean {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        return emailRegex.test(email);
    }

    return (
        <ThemeProvider theme={overrideTheme}>
            <div className="MainApp">
                <Container>
                    <Box className="centerCard">

                        <Typography align='left' variant="h6" gutterBottom fontWeight="bolder"
                                    sx={{marginBottom: '1em'}}>
                            Sign Up:
                        </Typography>

                        <form onSubmit={handleSignUpSubmit}>
                            <FormFieldInput label={'Enter Username'}
                                            type={'text'}
                                            value={username}
                                            onChange={handleUsernameChange}
                                            placeholder={''}
                                            autoComplete="username"
                                            name="username"
                            />

                            <FormFieldInput label={'Enter Email'}
                                            type={'email'}
                                            value={email}
                                            onChange={handleEmailChange}
                                            placeholder={''}
                                            autoComplete="email"
                                            name="email"
                            />

                            <PasswordField
                                label="Enter Password"
                                value={password}
                                onChange={handlePasswordChange}
                                placeholder={''}
                                autoComplete="new-password"
                                name="password"
                            />

                            {formMessage && (
                                <Alert severity={formMessageType === 'success' ? 'success' : 'error'}
                                       sx={{marginTop: '1em', marginBottom: '1em'}}>
                                    <Stack spacing={1}>
                                        <span>{formMessage}</span>
                                        {verificationInfo?.verificationUrl && (
                                            <MuiLink href={verificationInfo.verificationUrl} underline="hover">
                                                Verify account now
                                            </MuiLink>
                                        )}
                                    </Stack>
                                </Alert>
                            )
                            }

                            <Button variant="contained" color="primary" type="submit"
                                    sx={{marginTop: '1em', marginBottom: '1em', alignSelf: 'flex-start'}}>
                                Sign Up
                            </Button>
                            <Typography variant="body2">
                                Have an account? Login <Link to="/login">here</Link>.
                            </Typography>
                        </form>
                    </Box>
                </Container>
            </div>
        </ThemeProvider>
    )
}

export default SignUpForm;
