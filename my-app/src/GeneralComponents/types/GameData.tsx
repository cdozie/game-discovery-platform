export type GameData = {
    id: string;
    psnId: string;
    name: string;
    description: string;
    categories: string;
    platforms: string;
    price: string;
    url: string;
    releaseDate: string;
    provider: string;
    imageUrl: string;
    inWishlist: boolean;
};

export type GameDataWithInteractions = GameData & {
    interactions: string;
}