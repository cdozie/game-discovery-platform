import React from "react";
import '../Banner.scss';


type PictureBannerProps = {
    imageUrl: string;
    imageAlt: string;
};

function PictureBanner({imageUrl, imageAlt}: PictureBannerProps) {
    return (
        <div className="banner" style={{backgroundImage: `url(${imageUrl})`}}>
        </div>
    );
}

export default PictureBanner;