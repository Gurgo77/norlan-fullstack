/*
Payload DTO per l'invio della valutazione di un corso da parte del discente.
Contiene i punteggi assegnati alla docenza e ai contenuti, oltre al commento testuale.
*/
export interface FeedbackRequest {
	idUtente: number;
	idCorso: number;
	ratingDocenza: number;
	ratingContenuti: number;
	commento: string;
}