import React from 'react';
import '../App.scss';
import {Button, FormControl, Input, InputLabel, TextField} from '@mui/material';

function FormControlInput({label, type, value, onChange, placeholder}: any) {
    return (
        <FormControl sx={{marginBottom: '1em'}}>
            <InputLabel>{label}</InputLabel>
            <Input type={type} value={value} onChange={onChange} placeholder={placeholder}/>
        </FormControl>
    );
}

function SubmitButton({label, onClick}: any) {
    return (
        <Button variant="contained" color="primary" onClick={onClick}
                sx={{marginTop: '1em', marginBottom: '1em'}}>
            {label}
        </Button>
    );
}

const FormControlInputArrow: React.FC<{
    label: string;
    type: string;
    value: string;
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    placeholder?: string;
}> = ({label, type, value, onChange, placeholder}) => (
    <FormControl sx={{marginBottom: '1em'}}>
        <InputLabel>{label}</InputLabel>
        <Input type={type} value={value} onChange={onChange} placeholder={placeholder}/>
    </FormControl>
);

type FormFieldType = {
    label: string;
    type: string;
    value: string | number;
    onChange: (event: React.ChangeEvent<HTMLInputElement>) => void;
    placeholder: string;
    autoComplete: boolean;
};


function FormFieldInput({label, type, value, onChange, placeholder, autoComplete}: FormFieldType) {
    return (
        <TextField
            label={label}
            variant="outlined"
            fullWidth
            value={value}
            onChange={onChange}
            placeholder={placeholder}
            type={type || "text"}
            autoComplete={(autoComplete ? autoComplete.toString() : 'true')}
            sx={{marginBottom: '1em'}}
        />


    )
}


export default FormFieldInput;