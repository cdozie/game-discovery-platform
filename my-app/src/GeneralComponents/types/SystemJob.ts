export type SystemJob = {
    id: string;
    type: string;
    status: string;
    requestedBy: string;
    catalogSource?: string | null;
    relatedSyncRunId?: string | null;
    progressMessage?: string | null;
    resultSummary?: string | null;
    errorMessage?: string | null;
    createdAt: string;
    startedAt?: string | null;
    completedAt?: string | null;
    lastHeartbeatAt?: string | null;
};
