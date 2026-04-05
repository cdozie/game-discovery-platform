import apiClient from "./client";
import {AdminDashboard} from "../GeneralComponents/types/AdminDashboard";
import {AdminAuditLog} from "../GeneralComponents/types/AdminAuditLog";
import {AdminGame} from "../GeneralComponents/types/AdminGame";
import {AdminUser} from "../GeneralComponents/types/AdminUser";
import {CatalogDiscovery} from "../GeneralComponents/types/CatalogDiscovery";
import {CatalogDashboard, CatalogSyncRun} from "../GeneralComponents/types/CatalogDashboard";
import {GameComment} from "../GeneralComponents/types/GameComment";
import {GameData} from "../GeneralComponents/types/GameData";
import {GameSearchMetadata} from "../GeneralComponents/types/GameSearchMetadata";
import {GameSubmission} from "../GeneralComponents/types/GameSubmission";
import {GameSearchPage} from "../GeneralComponents/types/GameSearchPage";
import {MediaAsset} from "../GeneralComponents/types/MediaAsset";
import {OffsetPage} from "../GeneralComponents/types/OffsetPage";
import {SystemJob} from "../GeneralComponents/types/SystemJob";
import {UserProfile} from "../GeneralComponents/types/UserProfile";
import {WishlistType} from "../GeneralComponents/types/WishlistType";
import {GameStatusTypes} from "../GeneralComponents/types/GameStatusTypes";

export type VerificationResponse = {
    type: string;
    message: string;
    emailSent: boolean;
    verificationUrl?: string | null;
};

export async function getAuthStatus(): Promise<boolean> {
    const response = await apiClient.get("/auth/user/authentication-status");
    return !!response.data?.isAuthenticated;
}

export async function login(username: string, password: string): Promise<void> {
    await apiClient.post("/auth/login", {username, password});
}

export async function logout(): Promise<void> {
    await apiClient.post("/auth/logout");
}

export async function getMyProfile(): Promise<UserProfile> {
    const response = await apiClient.get("/account/profile");
    return response.data;
}

export async function updateMyProfile(payload: Partial<UserProfile>): Promise<UserProfile> {
    const response = await apiClient.put("/account/profile", payload);
    return response.data;
}

export async function uploadMedia(file: File, purpose: string): Promise<MediaAsset> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post("/media/upload", formData, {
        params: {purpose},
        headers: {'Content-Type': 'multipart/form-data'}
    });
    return response.data;
}

export async function updateMyPassword(currentPassword: string, newPassword: string): Promise<void> {
    await apiClient.put("/account/password", {currentPassword, newPassword});
}

export async function register(username: string, email: string, password: string): Promise<VerificationResponse> {
    const response = await apiClient.post("/auth/register", {username, email, password, roles: []});
    return response.data;
}

export async function submitGameSuggestion(payload: Partial<{
    name: string;
    description: string;
    categories: string;
    platforms: string;
    providerName: string;
    imageUrl: string;
    releaseDate: string;
    url: string;
}>): Promise<GameSubmission> {
    const response = await apiClient.post("/catalog/submissions", payload);
    return response.data;
}

export async function getMyGameSubmissions(): Promise<GameSubmission[]> {
    const response = await apiClient.get("/catalog/submissions/mine");
    return response.data;
}

export async function updateMyGameSubmission(submissionId: string, payload: Partial<{
    name: string;
    description: string;
    categories: string;
    platforms: string;
    providerName: string;
    imageUrl: string;
    releaseDate: string;
    url: string;
}>): Promise<GameSubmission> {
    const response = await apiClient.put(`/catalog/submissions/mine/${submissionId}`, payload);
    return response.data;
}

export async function withdrawMyGameSubmission(submissionId: string): Promise<GameSubmission> {
    const response = await apiClient.put(`/catalog/submissions/mine/${submissionId}/withdraw`);
    return response.data;
}

export async function getCatalogDashboard(): Promise<CatalogDashboard> {
    const response = await apiClient.get("/catalog/dashboard");
    return response.data;
}

export async function getCatalogDiscovery(): Promise<CatalogDiscovery> {
    const response = await apiClient.get("/catalog/discovery");
    return response.data;
}

export async function triggerCatalogSync(payload: Partial<{ catalogSource: string; pageSize: number; maxBatches: number; maxRecords: number }> = {}): Promise<CatalogSyncRun> {
    const response = await apiClient.post("/catalog/sync", payload);
    return response.data;
}

export async function getLatestCatalogSync(): Promise<CatalogSyncRun | null> {
    const response = await apiClient.get("/catalog/sync");
    return response.data;
}

export async function getAdminDashboard(): Promise<AdminDashboard> {
    const response = await apiClient.get("/admin/dashboard");
    return response.data;
}

export async function getAdminSubmissions(status?: string, limit = 12, offset = 0): Promise<OffsetPage<GameSubmission>> {
    const response = await apiClient.get("/admin/submissions", {
        params: {
            status: status || undefined,
            limit,
            offset
        }
    });
    return {
        offset: response.data?.offset ?? offset,
        limit: response.data?.limit ?? limit,
        hasMore: !!response.data?.hasMore,
        items: Array.isArray(response.data?.items) ? response.data.items : []
    };
}

export async function getAdminAuditLogs(limit = 80): Promise<AdminAuditLog[]> {
    const response = await apiClient.get("/admin/audit", {params: {limit}});
    return response.data;
}

export async function getAdminJobs(limit = 40): Promise<SystemJob[]> {
    const response = await apiClient.get("/admin/jobs", {params: {limit}});
    return response.data;
}

export async function queueAdminJob(payload: Partial<{
    jobType: string;
    catalogSource: string;
    pageSize: number;
    maxBatches: number;
    maxRecords: number;
    startOffset: number;
}>): Promise<SystemJob> {
    const response = await apiClient.post("/admin/jobs", payload);
    return response.data;
}

export async function reviewAdminSubmission(submissionId: string, payload: { status: string; reviewNotes?: string | null }): Promise<GameSubmission> {
    const response = await apiClient.post(`/admin/submissions/${submissionId}/review`, payload);
    return response.data;
}

export async function updateAdminSubmission(submissionId: string, payload: Partial<{
    name: string;
    description: string;
    categories: string;
    platforms: string;
    providerName: string;
    imageUrl: string;
    releaseDate: string;
    url: string;
}>): Promise<GameSubmission> {
    const response = await apiClient.put(`/admin/submissions/${submissionId}`, payload);
    return response.data;
}

export async function getAdminUsers(query?: string, onlyBanned = false): Promise<AdminUser[]> {
    const response = await apiClient.get("/admin/users", {params: {query: query || undefined, onlyBanned: onlyBanned || undefined}});
    return response.data;
}

export async function banAdminUser(userId: string, payload: { durationDays?: number | null; permanent?: boolean; reason?: string | null }): Promise<AdminUser> {
    const response = await apiClient.put(`/admin/users/${userId}/ban`, payload);
    return response.data;
}

export async function unbanAdminUser(userId: string): Promise<AdminUser> {
    const response = await apiClient.put(`/admin/users/${userId}/unban`);
    return response.data;
}

export async function getAdminGames(query?: string, limit = 24): Promise<AdminGame[]> {
    const response = await apiClient.get("/admin/games", {params: {query: query || undefined, limit}});
    return response.data;
}

export async function getAdminGame(gameId: string): Promise<AdminGame> {
    const response = await apiClient.get(`/admin/games/${gameId}`);
    return response.data;
}

export async function createAdminGame(payload: Partial<{
    name: string;
    description: string;
    categories: string;
    platforms: string;
    providerName: string;
    imageUrl: string;
    releaseDate: string;
    url: string;
    price: string;
    syncProtectedFields: string[];
}>): Promise<AdminGame> {
    const response = await apiClient.post("/admin/games", payload);
    return response.data;
}

export async function updateAdminGame(gameId: string, payload: Partial<{
    name: string;
    description: string;
    categories: string;
    platforms: string;
    providerName: string;
    imageUrl: string;
    releaseDate: string;
    url: string;
    price: string;
    syncProtectedFields: string[];
}>): Promise<AdminGame> {
    const response = await apiClient.put(`/admin/games/${gameId}`, payload);
    return response.data;
}

export async function deleteAdminGame(gameId: string): Promise<void> {
    await apiClient.delete(`/admin/games/${gameId}`);
}

export async function setAdminSpotlightGame(gameId: string): Promise<AdminDashboard> {
    const response = await apiClient.put(`/admin/spotlight/${gameId}`);
    return response.data;
}

export async function clearAdminSpotlightGame(): Promise<AdminDashboard> {
    const response = await apiClient.delete('/admin/spotlight');
    return response.data;
}

export async function getGame(gameId: string): Promise<GameData> {
    const response = await apiClient.get(`/auth/games/${gameId}`);
    return response.data;
}

function normalizeComment(comment: Partial<GameComment>): GameComment {
    return {
        id: String(comment.id ?? ''),
        parentCommentId: comment.parentCommentId ?? null,
        content: comment.content ?? '',
        authorUsername: comment.authorUsername ?? 'unknown',
        authorDisplayName: comment.authorDisplayName ?? comment.authorUsername ?? 'Unknown user',
        authorAvatarUrl: comment.authorAvatarUrl ?? null,
        authorAdmin: !!comment.authorAdmin,
        createdAt: comment.createdAt ?? '',
        lastModified: comment.lastModified ?? null,
        edited: !!comment.edited,
        deleted: !!comment.deleted,
        ownedByCurrentUser: !!comment.ownedByCurrentUser,
        replies: Array.isArray(comment.replies) ? comment.replies.map(normalizeComment) : []
    };
}

export async function getGameComments(gameId: string, limit = 12, offset = 0): Promise<OffsetPage<GameComment>> {
    const response = await apiClient.get(`/auth/games/${gameId}/comments`, {
        params: {limit, offset}
    });
    const rawComments = Array.isArray(response.data?.items) ? response.data.items : [];
    return {
        offset: response.data?.offset ?? offset,
        limit: response.data?.limit ?? limit,
        hasMore: !!response.data?.hasMore,
        items: rawComments.map(normalizeComment)
    };
}

export async function createGameComment(gameId: string, content: string, parentCommentId?: string | null): Promise<GameComment> {
    const response = await apiClient.post(`/auth/games/${gameId}/comments`, {content, parentCommentId});
    return normalizeComment(response.data);
}

export async function updateGameComment(gameId: string, commentId: string, content: string): Promise<GameComment> {
    const response = await apiClient.put(`/auth/games/${gameId}/comments/${commentId}`, {content});
    return normalizeComment(response.data);
}

export async function deleteGameComment(gameId: string, commentId: string): Promise<void> {
    await apiClient.delete(`/auth/games/${gameId}/comments/${commentId}`);
}

export async function getRandomGameId(): Promise<string> {
    const response = await apiClient.get("/auth/games/random-id");
    return response.data.id;
}

export async function searchGames(query: string, limit = 20): Promise<GameData[]> {
    const response = await apiClient.get("/auth/games/search", {
        params: {query, limit}
    });
    return response.data;
}

export async function searchGamesPage(query: string,
                                      limit = 24,
                                      offset = 0,
                                      filters: Partial<{
                                          platform: string;
                                          category: string;
                                          publisher: string;
                                          publisherExact: boolean;
                                          catalogSource: string;
                                          hasDescription: boolean;
                                          hasImage: boolean;
                                          sort: string;
                                      }> = {}): Promise<GameSearchPage> {
    const response = await apiClient.get("/auth/games/search-page", {
        params: {query, limit, offset, ...filters}
    });
    return response.data;
}

export async function getGameSearchMetadata(options: Partial<{
    publisherQuery: string;
    platformQuery: string;
    categoryQuery: string;
    limit: number;
}> = {}): Promise<GameSearchMetadata> {
    const response = await apiClient.get("/auth/games/search-metadata", {
        params: {
            publisherQuery: options.publisherQuery || undefined,
            platformQuery: options.platformQuery || undefined,
            categoryQuery: options.categoryQuery || undefined,
            limit: options.limit || undefined
        }
    });
    return response.data;
}

export async function recordGameInteraction(gameId: string): Promise<void> {
    await apiClient.post("/auth/games/interactions", {gameId});
}

export async function getWishlist(): Promise<WishlistType[]> {
    const response = await apiClient.get("/auth/games/wishlist");
    return response.data;
}

export async function getWishlistEntry(gameId: string): Promise<WishlistType> {
    const response = await apiClient.get(`/auth/games/wishlist/${gameId}`);
    return response.data;
}

function normalizeOutgoingRating(rating: number | string | null): number | null {
    if (rating === null || rating === undefined || rating === '') {
        return null;
    }

    const parsedRating = typeof rating === 'number' ? rating : Number(rating);
    return Number.isFinite(parsedRating) ? parsedRating : null;
}

export async function upsertWishlist(gameId: string,
                                     status: GameStatusTypes,
                                     rating: number | string | null,
                                     selectedPlatform?: string | null): Promise<void> {
    await apiClient.post("/auth/wishlist", {gameId, status, rating: normalizeOutgoingRating(rating), selectedPlatform});
}

export async function removeWishlistEntry(gameId: string): Promise<void> {
    await apiClient.delete(`/auth/wishlist/${gameId}`);
}

export async function updateWishlistRating(gameId: string, rating: number | string | null): Promise<void> {
    await apiClient.post(`/auth/games/${gameId}/rating`, {rating: normalizeOutgoingRating(rating)});
}

export async function updateWishlistStatus(gameId: string, status: GameStatusTypes): Promise<void> {
    await apiClient.put(`/auth/games/${gameId}/status/${status}`);
}
