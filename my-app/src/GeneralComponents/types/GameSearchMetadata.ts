export type SearchMetadataValue = {
    value: string;
    count: number;
};

export type GameSearchMetadata = {
    publishers: SearchMetadataValue[];
    platforms: SearchMetadataValue[];
    categories: SearchMetadataValue[];
    catalogSources: SearchMetadataValue[];
};
