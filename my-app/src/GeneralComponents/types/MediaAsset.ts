export type MediaAsset = {
    publicId: string;
    purpose: string;
    contentType: string;
    originalFilename?: string | null;
    sizeBytes: number;
    url: string;
    createdAt?: string | null;
};
