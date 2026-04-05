export type AdminUser = {
    id: string;
    username: string;
    email: string;
    displayName: string;
    favoritePlatform?: string | null;
    roles: string[];
    banned: boolean;
    permanentBan: boolean;
    banReason?: string | null;
    bannedByUserId?: string | null;
    banStartedAt?: string | null;
    banExpiresAt?: string | null;
    createdAt?: string | null;
    lastModified?: string | null;
};
