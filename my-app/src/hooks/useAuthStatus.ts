import {useEffect, useState} from "react";
import {getAuthStatus} from "../api/games";

export function useAuthStatus() {
    const [isAuthenticated, setIsAuthenticated] = useState<boolean | null>(null);

    useEffect(() => {
        let isMounted = true;

        getAuthStatus()
            .then(authenticated => {
                if (isMounted) {
                    setIsAuthenticated(authenticated);
                }
            })
            .catch(() => {
                if (isMounted) {
                    setIsAuthenticated(false);
                }
            });

        return () => {
            isMounted = false;
        };
    }, []);

    return {isAuthenticated, setIsAuthenticated};
}
