import axios from "axios";

const apiClient = axios.create({
    baseURL: "/api",
    timeout: 30000,
    withCredentials: true
});

export function getErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        return (error.response?.data as { message?: string } | undefined)?.message || error.message;
    }

    if (error instanceof Error) {
        return error.message;
    }

    return "An unexpected error occurred.";
}

export default apiClient;
