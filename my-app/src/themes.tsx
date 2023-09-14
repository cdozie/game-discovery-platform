import React from 'react';
import {createTheme} from "@mui/material";

const overrideTheme = createTheme({
        typography: {
            fontFamily: [
                'ui-sans-serif',
                'system-ui',
                '-apple-system',
                'BlinkMacSystemFont',
                'Segoe UI',
                'Roboto',
                'Helvetica Neue',
            ].join(','),
        },
        components: {
            MuiTypography: {
                styleOverrides: {
                    root: {
                        fontFamily: 'ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica Neue',

                    }
                }
            }
        },
        palette: {
            primary: {
                main: '#000000',
                light: '#000000',
                dark: '#000000',
                contrastText: '#ffffff',
            }
        }
    }
);

export default overrideTheme;
