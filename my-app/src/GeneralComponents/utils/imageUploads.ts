export const ACCEPTED_UPLOAD_IMAGE_TYPES = new Set(['image/png', 'image/jpeg', 'image/jpg', 'image/webp', 'image/gif']);

type InlineImageOptions = {
    maxDimension: number;
    maxDataUrlLength: number;
    maxGifBytes: number;
    oversizedGifMessage: string;
    oversizedImageMessage: string;
};

type UploadImageOptions = {
    maxDimension: number;
    maxBlobBytes: number;
    maxGifBytes: number;
    oversizedGifMessage: string;
    oversizedImageMessage: string;
};

function readFileAsDataUrl(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => {
            if (typeof reader.result === 'string') {
                resolve(reader.result);
                return;
            }
            reject(new Error('Could not read the selected image.'));
        };
        reader.onerror = () => reject(new Error('Could not read the selected image.'));
        reader.readAsDataURL(file);
    });
}

function loadImage(dataUrl: string): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
        const image = new Image();
        image.onload = () => resolve(image);
        image.onerror = () => reject(new Error('Could not process the selected image.'));
        image.src = dataUrl;
    });
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string, quality?: number): Promise<Blob> {
    return new Promise((resolve, reject) => {
        canvas.toBlob(blob => {
            if (blob) {
                resolve(blob);
                return;
            }
            reject(new Error('Could not process the selected image.'));
        }, type, quality);
    });
}

export async function buildOptimizedInlineImageDataUrl(file: File, options: InlineImageOptions): Promise<string> {
    if (file.type === 'image/gif') {
        if (file.size > options.maxGifBytes) {
            throw new Error(options.oversizedGifMessage);
        }
        return readFileAsDataUrl(file);
    }

    const originalDataUrl = await readFileAsDataUrl(file);
    const image = await loadImage(originalDataUrl);
    const scale = Math.min(1, options.maxDimension / Math.max(image.width, image.height));
    const width = Math.max(1, Math.round(image.width * scale));
    const height = Math.max(1, Math.round(image.height * scale));
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');

    if (!context) {
        return originalDataUrl;
    }

    canvas.width = width;
    canvas.height = height;
    context.drawImage(image, 0, 0, width, height);

    let normalizedDataUrl = canvas.toDataURL('image/webp', 0.88);
    if (normalizedDataUrl.length > options.maxDataUrlLength) {
        normalizedDataUrl = canvas.toDataURL('image/jpeg', 0.86);
    }
    if (normalizedDataUrl.length > options.maxDataUrlLength) {
        throw new Error(options.oversizedImageMessage);
    }

    return normalizedDataUrl;
}

export async function buildOptimizedUploadFile(file: File, options: UploadImageOptions): Promise<File> {
    if (file.type === 'image/gif') {
        if (file.size > options.maxGifBytes) {
            throw new Error(options.oversizedGifMessage);
        }
        return file;
    }

    const originalDataUrl = await readFileAsDataUrl(file);
    const image = await loadImage(originalDataUrl);
    const scale = Math.min(1, options.maxDimension / Math.max(image.width, image.height));
    const width = Math.max(1, Math.round(image.width * scale));
    const height = Math.max(1, Math.round(image.height * scale));
    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d');

    if (!context) {
        return file;
    }

    canvas.width = width;
    canvas.height = height;
    context.drawImage(image, 0, 0, width, height);

    let blob = await canvasToBlob(canvas, 'image/webp', 0.88);
    let extension = 'webp';
    let contentType = 'image/webp';

    if (blob.size > options.maxBlobBytes) {
        blob = await canvasToBlob(canvas, 'image/jpeg', 0.86);
        extension = 'jpg';
        contentType = 'image/jpeg';
    }

    if (blob.size > options.maxBlobBytes) {
        throw new Error(options.oversizedImageMessage);
    }

    const baseName = file.name.replace(/\.[^.]+$/, '') || 'upload';
    return new File([blob], `${baseName}.${extension}`, {
        type: contentType,
        lastModified: Date.now()
    });
}
