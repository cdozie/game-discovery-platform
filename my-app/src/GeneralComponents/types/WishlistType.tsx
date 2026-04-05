import {GameData} from "./GameData";
import {GameStatusTypes} from "./GameStatusTypes";

export interface WishlistType {
    id: string | null;
    gameinfo: GameData
    rating: number | string | null;
    status: GameStatusTypes;
    selectedPlatform?: string | null;
}
