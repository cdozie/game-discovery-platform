export type UserProfile = {
    id: string;
    username: string;
    email: string;
    displayName: string;
    bio?: string | null;
    avatarUrl?: string | null;
    favoritePlatform?: string | null;
    roles?: string[] | null;
    admin?: boolean;
    createdAt?: string | null;
    lastModified?: string | null;
};
