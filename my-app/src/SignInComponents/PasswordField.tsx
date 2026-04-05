import {IconButton, InputAdornment, TextField} from "@mui/material";
import {Visibility, VisibilityOff} from "@mui/icons-material";
import React from "react";

type PasswordFieldProps = {
    label: string;
    placeholder: string;
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    value: string;
    autoComplete?: string;
    name?: string;
};

function PasswordField({label, placeholder, onChange, value, autoComplete, name}: PasswordFieldProps) {

    const [showPassword, setShowPassword] = React.useState(false);

    const handleClickShowPassword = () => setShowPassword((show) => !show);
    const handleMouseDownPassword = (event: React.MouseEvent<HTMLButtonElement>) => event.preventDefault();
    return (
        <TextField
            label={label}
            variant="outlined"
            fullWidth
            value={value}
            onChange={onChange}
            placeholder={placeholder}
            type={showPassword ? 'text' : 'password'}
            autoComplete={autoComplete}
            name={name}
            sx={{marginBottom: '1em'}}
            InputProps={{
                endAdornment: (
                    <InputAdornment position="end">
                        <IconButton
                            aria-label="toggle password visibility"
                            onClick={handleClickShowPassword}
                            onMouseDown={handleMouseDownPassword}
                            edge="end"
                        >
                            {showPassword ? <VisibilityOff/> : <Visibility/>}
                        </IconButton>
                    </InputAdornment>
                )
            }}
        />
    )
}

export default PasswordField;
