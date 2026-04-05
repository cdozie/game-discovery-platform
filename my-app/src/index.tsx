import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import 'bootstrap/dist/css/bootstrap.css';
import {BrowserRouter, Navigate, Route, Routes} from "react-router-dom";
import {ThemeProvider} from "@mui/material/styles";
import LoginForm from './SignInComponents/LoginForm';
import SignUpForm from "./SignInComponents/SignUpForm";
import HomePage from "./MainComponents/HomePage";
import GameMainPage from "./MainComponents/GameMainPage";
import ResponsiveAppBar from "./MainComponents/ResponsiveAppBar";
import GameList from "./MainComponents/UserList";
import DiscoveryPage from "./MainComponents/DiscoveryPage";
import AccountSettingsPage from "./MainComponents/AccountSettingsPage";
import SearchPage from "./MainComponents/SearchPage";
import SubmitGamePage from "./MainComponents/SubmitGamePage";
import AdminPortalPage from "./MainComponents/AdminPortalPage";
import overrideTheme from "./themes";
import {LoadingProvider} from "./GeneralComponents/LoadingContext";
import {useAuthStatus} from "./hooks/useAuthStatus";
import reportWebVitals from './reportWebVitals';

const root = ReactDOM.createRoot(
    document.getElementById('root') as HTMLElement
);

function AppFrame({children, showAppBar = true}: { children: React.ReactNode; showAppBar?: boolean }) {
    return (
        <div id="root">
            <main>
                <div className="MainApp">
                    <ThemeProvider theme={overrideTheme}>
                        <LoadingProvider>
                            {showAppBar && <ResponsiveAppBar/>}
                            {children}
                        </LoadingProvider>
                    </ThemeProvider>
                </div>
            </main>
        </div>
    );
}

function ProtectedElement({children}: { children: React.ReactNode }): React.ReactElement | null {
    const {isAuthenticated} = useAuthStatus();

    if (isAuthenticated === null) {
        return null;
    }

    return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace/>;
}

root.render(
    <BrowserRouter basename='/api'>
        <Routes>
            <Route
                path="/login"
                element={
                    <AppFrame showAppBar={false}>
                        <LoginForm/>
                    </AppFrame>
                }
            />
            <Route
                path="/register"
                element={
                    <AppFrame showAppBar={false}>
                        <SignUpForm/>
                    </AppFrame>
                }
            />
            <Route
                path="/"
                element={
                    <AppFrame>
                        <ProtectedElement>
                            <HomePage/>
                        </ProtectedElement>
                    </AppFrame>
                }
            />
            <Route
                path="/discover"
                element={
                    <AppFrame>
                        <ProtectedElement>
                            <DiscoveryPage/>
                        </ProtectedElement>
                    </AppFrame>
                }
            />
            <Route
                path="/search"
                element={
                    <AppFrame>
                        <ProtectedElement>
                            <SearchPage/>
                        </ProtectedElement>
                    </AppFrame>
                }
            />
            <Route
                path="/games/:gameId"
                element={
                    <AppFrame>
                        <ProtectedElement>
                            <GameMainPage/>
                        </ProtectedElement>
                    </AppFrame>
                }
            />
            <Route
                path="/mylist"
                element={
                    <AppFrame>
                        <ProtectedElement>
                            <GameList/>
                        </ProtectedElement>
                    </AppFrame>
                }
            />
            <Route
                path="/settings"
                element={
                    <AppFrame>
                        <ProtectedElement>
                            <AccountSettingsPage/>
                        </ProtectedElement>
                    </AppFrame>
                }
            />
            <Route
                path="/submit-game"
                element={
                    <AppFrame>
                        <ProtectedElement>
                            <SubmitGamePage/>
                        </ProtectedElement>
                    </AppFrame>
                }
            />
            <Route
                path="/admin"
                element={
                    <AppFrame>
                        <ProtectedElement>
                            <AdminPortalPage/>
                        </ProtectedElement>
                    </AppFrame>
                }
            />
            <Route
                path="*"
                element={
                    <AppFrame>
                        <ProtectedElement>
                            <HomePage/>
                        </ProtectedElement>
                    </AppFrame>
                }
            />
        </Routes>
    </BrowserRouter>
);

reportWebVitals();
