import {GameData} from "./GameData";

export type CatalogDiscovery = {
    preferredPlatform?: string | null;
    spotlightGame: GameData | null;
    featuredGames: GameData[];
    newAndNoteworthy: GameData[];
    communityFavorites: GameData[];
    surpriseGames: GameData[];
};
