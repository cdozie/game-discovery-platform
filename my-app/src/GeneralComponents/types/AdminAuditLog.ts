export type AdminAuditLog = {
    id: string;
    actorUserId?: string | null;
    actorUsername?: string | null;
    action: string;
    entityType: string;
    entityId?: string | null;
    summary: string;
    details?: Record<string, unknown> | null;
    createdAt?: string | null;
};
