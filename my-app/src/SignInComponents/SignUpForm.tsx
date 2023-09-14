import React from 'react';

import '../App.scss';
import {ThemeProvider} from "@mui/material/styles";
import overrideTheme from "../themes";
import {Alert, Box, Button, Container, Typography} from "@mui/material";
import {Link} from "react-router-dom";
import axios from "axios";
import FormFieldInput from "./FormComponents";
import PasswordField from "./PasswordField";

function SignUpForm() {
    const [username, setUsername] = React.useState('');
    const [password, setPassword] = React.useState('');
    const [email, setEmail] = React.useState('');
    const [formMessage, setFormMessage] = React.useState('');
    const [formMessageType, setFormMessageType] = React.useState('success');

    const handleUsernameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFormMessage('')
        setUsername(e.target.value);
    }
    const handlePasswordChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setFormMessage('')
        setPassword(e.target.value);
    }
    const handleEmailChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const emailInput = e.target.value;
        setEmail(emailInput);
        if (isValidEmail(emailInput)) {
            setFormMessage(''); // clear error message if email is valid
        } else {
            setFormMessage('Invalid email address');
            setFormMessageType('error');

        }


    }

    const handleSignUpSubmit = (event: React.FormEvent) => {
        event.preventDefault();
        axios.post("/api/auth/register", {
            username: username,
            password: password,
            email: email,
            roles: ["user"]
        })
            .then(response => {
                if (response.data) {
                    setFormMessage(response.data.message || '');
                    setFormMessageType('success')
                }
            })
            .catch(error => {
                if (error.response) {
                    setFormMessage(error.response.data.message || error.message);
                    setFormMessageType('error');
                }
            });
    }

    function isValidEmail(email: string): boolean {
        const emailRegex = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,4}$/;
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

                        <FormFieldInput label={'Enter Username'}
                                        type={'text'}
                                        value={username}
                                        onChange={handleUsernameChange}
                                        placeholder={''}
                                        autoComplete={true}
                        />

                        <FormFieldInput label={'Enter Email'}
                                        type={'email'}
                                        value={email}
                                        onChange={handleEmailChange}
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
                                   sx={{marginTop: '1em', marginBottom: '1em'}}>
                                {formMessage}
                            </Alert>
                        )
                        }

                        <Button variant="contained" color="primary" onClick={handleSignUpSubmit}
                                sx={{marginTop: '1em', marginBottom: '1em', alignSelf: 'flex-start'}}>
                            Sign Up
                        </Button>
                        <Typography variant="body2">
                            Have an account? Login <Link to="/login">here</Link>.
                        </Typography>
                    </Box>
                </Container>
            </div>
        </ThemeProvider>
    )
}

export default SignUpForm;
