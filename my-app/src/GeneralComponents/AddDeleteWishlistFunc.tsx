import axios from "axios/index";
import {Severity} from "./PopupMessage";
import React from "react";


export type wishlistAction = 'REMOVE' | 'ADD';

interface wishlistResponse {
    message: string;
    severity: Severity;
}


export const toggleWishlist = (wishlistAction: wishlistAction, gameId: string, setMessageState: (value: React.SetStateAction<wishlistResponse>) => void,
) => {


    const action = wishlistAction === 'ADD' ? 'addToList' : 'removeFromList';
    axios.post(`/api/auth/${action}`, {gameId})
        .then(response => {
            const message = action === 'addToList' ? 'Game added to wishlist' : 'Game removed from wishlist';
            setMessageState({
                message: response?.data?.message || message,
                severity: Severity.SUCCESS
            });

        })
        .catch(error => {
            setMessageState({
                message: error?.response?.data?.message || error.message,
                severity: Severity.ERROR
            });
        });

};