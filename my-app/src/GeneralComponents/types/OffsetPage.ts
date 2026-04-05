export type OffsetPage<T> = {
    offset: number;
    limit: number;
    hasMore: boolean;
    items: T[];
};
