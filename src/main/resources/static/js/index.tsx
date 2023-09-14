import React from "react";
import {createRoot} from 'react-dom/client';
import "bootstrap/dist/css/bootstrap.css";
import App from "./App";

const RootElement = document.getElementById("root");
if (RootElement) {
    const root = createRoot(RootElement);
    root.render(<App/>);
}