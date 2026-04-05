export const COMMON_PLATFORM_OPTIONS = [
    'PS5',
    'PS4',
    'PS3',
    'PS2',
    'PS1',
    'PS VR2',
    'PS VR',
    'PS Vita',
    'PSP',
    'Xbox Series X|S',
    'Xbox One',
    'Xbox 360',
    'Xbox',
    'Nintendo Switch',
    'Wii U',
    'Wii',
    'Nintendo 3DS',
    'Nintendo DS',
    'PC',
    'Mac',
    'Linux',
    'iOS',
    'Android'
];

export function extractPlatformOptions(platforms?: string | null): string[] {
    if (!platforms) {
        return [];
    }

    return Array.from(new Set(
        platforms
            .split(',')
            .map(platform => platform.trim())
            .filter(Boolean)
    ));
}

export function requiresPlatformSelection(platforms?: string | null): boolean {
    return extractPlatformOptions(platforms).length > 1;
}
