export type AdminDashboard = {
    totalUsers: number;
    activeBans: number;
    totalGames: number;
    pendingSubmissions: number;
    approvedSubmissions: number;
    rejectedSubmissions: number;
    spotlightGameId?: string | null;
    spotlightGameName?: string | null;
};
