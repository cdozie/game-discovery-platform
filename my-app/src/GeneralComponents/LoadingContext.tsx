import React, {ReactNode, useContext, useState} from "react";


interface LoadingContextType {
    loading: boolean;
    setLoading: React.Dispatch<React.SetStateAction<boolean>>;
}

const LoadingContext = React.createContext<LoadingContextType | undefined>(undefined);
export const useLoading = () => {

    return useContext(LoadingContext);
};

interface LoadingProviderProps {
    children: ReactNode;
}


export const LoadingProvider: React.FC<LoadingProviderProps> = ({children}: LoadingProviderProps) => {
    const [loading, setLoading] = useState<boolean>(false);

    return (
        <LoadingContext.Provider value={{loading, setLoading}}>
            {children}
        </LoadingContext.Provider>
    );
};