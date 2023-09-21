export enum GameStatusTypes {
    COMPLETED = "Completed",
    PLAYING = "Playing",
    DROPPED = "Dropped",
    PLAN_TO_PLAY = "Plan to Play",
    ON_HOLD = "On Hold",
    REPLAYING = "Replaying",
    WATCHING = "Watching",
    WANTED = "Wanted"
}

export function getStatusDisplayName(statusKey: keyof typeof GameStatusTypes): string {
    return GameStatusTypes[statusKey];
}