/*
Disabilita il Rendering lato Server (SSR) per questa pagina o layout,
forzando il rendering esclusivamente lato client (CSR).
Utile per componenti che dipendono pesantemente da browser-APIs (es. window, localStorage, WebSocket)
o per dashboard protette dove la generazione server-side non è necessaria.
*/
export const ssr = false;