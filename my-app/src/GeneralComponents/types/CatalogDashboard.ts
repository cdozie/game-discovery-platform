import {GameData, GameDataWithInteractions} from "./GameData";

export type CatalogSyncRun = {
    id: string;
    status: string;
    catalogSource: string;
    triggeredBy: string;
    startedAt: string;
    completedAt?: string | null;
    requestedPageSize?: number | null;
    requestedMaxBatches?: number | null;
    requestedMaxRecords?: number | null;
    requestedStartOffset?: number | null;
    currentOffset?: number | null;
    completedBatches?: number | null;
    lastCheckpointAt?: string | null;
    processedCount: number;
    createdCount: number;
    updatedCount: number;
    unchangedCount: number;
    failedCount: number;
    message?: string | null;
};

export type CatalogDashboard = {
    totalGames: number;
    totalUsers: number;
    totalWishlistEntries: number;
    syncInProgress: boolean;
    lastSync: CatalogSyncRun | null;
    latestGames: GameData[];
    popularGames: GameData[];
};
