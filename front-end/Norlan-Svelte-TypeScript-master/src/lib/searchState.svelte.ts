/*
Stato reattivo globale per la gestione della stringa di ricerca corrente.
Permette la sincronizzazione bidirezionale tra i componenti di input e le tabelle/liste filtrate.
*/
export const searchState = $state({
	query: ''
});