import {GameData} from "./GameData";

export type GameSearchPage = {
    query: string;
    offset: number;
    limit: number;
    hasMore: boolean;
    items: GameData[];
};
