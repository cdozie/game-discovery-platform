export type ModalMainProps = {
    open: boolean;
    onClose: () => void;
}

export type ModalMainPropsWithId = ModalMainProps & {
    gameId: string;
}
export type ModalMainPropsWithIdAndUpdate = ModalMainPropsWithId & {
    updateGameInWishlist: (gameId: string, inWishlist: boolean) => void;

}