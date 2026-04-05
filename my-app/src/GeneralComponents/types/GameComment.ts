export type GameComment = {
    id: string;
    parentCommentId?: string | null;
    content: string;
    authorUsername: string;
    authorDisplayName: string;
    authorAvatarUrl?: string | null;
    authorAdmin?: boolean;
    createdAt: string;
    lastModified?: string | null;
    edited: boolean;
    deleted: boolean;
    ownedByCurrentUser: boolean;
    replies: GameComment[];
};
