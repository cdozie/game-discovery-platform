import React, {createContext, useEffect, useState} from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import LoginForm from './SignInComponents/LoginForm';
import reportWebVitals from './reportWebVitals';
import 'bootstrap/dist/css/bootstrap.css';
import {BrowserRouter, Navigate, Route, Routes} from "react-router-dom";
import SignUpForm from "./SignInComponents/SignUpForm";
import HomePage from "./MainComponents/HomePage";
import axios from "axios";
import GameMainPage from "./MainComponents/GameMainPage";
import ResponsiveAppBar from "./MainComponents/ResponsiveAppBar";
import overrideTheme from "./themes";
import {ThemeProvider} from "@mui/material/styles";
import GameList from "./MainComponents/UserList";
import {LoadingProvider} from "./GeneralComponents/LoadingContext";

const root = ReactDOM.createRoot(
    document.getElementById('root') as HTMLElement
);

export const useAuth = () => {
    return React.useContext(AuthContext);
}
const AuthContext = createContext({
    isAuthenticated: false,
    setIsAuthenticated: (auth: boolean) => {
    }
});

// function LoginRoute() {
//     const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);
//
//     useEffect(() => {
//         axios.get('/api/auth/user/authentication-status')
//             .then(response => {
//                 setIsAuthenticated(response.data.isAuthenticated);
//             }).catch(error => {
//             setIsAuthenticated(false);
//         });
//     }, []);
//
//     if (isAuthenticated === null) {
//         return <Navigate to="/" replace/>;
//     }
//
//     console.log(isAuthenticated)
//     if (isAuthenticated) {
//         return <Navigate to="/" replace/>;
//     }
//     return <LoginForm/>;
// }

function AuthenticatedInaccessible({children}: { children: React.ReactNode }): React.ReactElement | null {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);

    useEffect(() => {
        axios.get('/api/auth/user/authentication-status')
            .then(response => {
                setIsAuthenticated(response.data.isAuthenticated);
            }).catch(error => {
            setIsAuthenticated(false);
        });
    }, []);

    if (isAuthenticated === null) {
        return null;
    }

    if (isAuthenticated) {
        return <Navigate to="/" replace/>;
    }
    return <>{children}</>
}

function Layout({children}: { children: React.ReactNode }) {
    return (
        <div id="root">
            <main>
                <div className="MainApp">
                    <ThemeProvider theme={overrideTheme}>

                        <LoadingProvider>
                            <ResponsiveAppBar/>
                            {children}
                        </LoadingProvider>

                    </ThemeProvider>
                </div>
            </main>
        </div>
    )
}

function LayoutNoAppBar({children}: { children: React.ReactNode }) {
    return (
        <div id="root">
            <main>
                <div className="MainApp">
                    <ThemeProvider theme={overrideTheme}>
                        <LoadingProvider>
                            {children}
                        </LoadingProvider>
                    </ThemeProvider>
                </div>
            </main>
        </div>
    )
}

function ProtectedElement({children}: { children: React.ReactNode }): React.ReactElement | null {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);

    useEffect(() => {
        axios.get('/api/auth/user/authentication-status')
            .then(response => {
                setIsAuthenticated(response.data.isAuthenticated);
            }).catch(error => {
            setIsAuthenticated(false);
        });
    }, []);

    if (isAuthenticated === null) {
        return null;
    }

    return isAuthenticated ? <>{children}</> : <Navigate to="/login"/>;
}


root.render(
    // <React.StrictMode>
    <BrowserRouter basename='/api'>
        <Routes>
            <Route path="/login" element={
                <LayoutNoAppBar>
                    <AuthenticatedInaccessible>
                        <LoginForm/>
                    </AuthenticatedInaccessible>
                </LayoutNoAppBar>}/>
            <Route path="/register" element={
                <AuthenticatedInaccessible>
                    <SignUpForm/>
                </AuthenticatedInaccessible>
            }/>
            <Route path="/" element={<Layout><ProtectedElement> <HomePage/> </ProtectedElement></Layout>}/>
            <Route path="/games/:gameId"
                   element={<Layout><ProtectedElement> <GameMainPage/> </ProtectedElement></Layout>}/>
            <Route path="/mylist" element={<Layout><ProtectedElement> <GameList/> </ProtectedElement></Layout>}/>
            <Route path="*" element={<Layout><ProtectedElement> <HomePage/> </ProtectedElement></Layout>}/>

            {/* Add any other routes below as PrivateRoute */}
        </Routes>
    </BrowserRouter>
);


// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
