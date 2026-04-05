import React from 'react';
import '../App.scss';
import {TextField} from '@mui/material';

type FormFieldType = {
    label: string;
    type: string;
    value: string | number;
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    placeholder: string;
    autoComplete?: string;
    name?: string;
};


function FormFieldInput({label, type, value, onChange, placeholder, autoComplete, name}: FormFieldType) {
    return (
        <TextField
            label={label}
            variant="outlined"
            fullWidth
            value={value}
            onChange={onChange}
            placeholder={placeholder}
            type={type || "text"}
            autoComplete={autoComplete}
            name={name}
            sx={{marginBottom: '1em'}}
        />


    )
}


export default FormFieldInput;
