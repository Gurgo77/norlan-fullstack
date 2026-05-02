export interface FeedbackRequest {
	idUtente: number;
	idCorso: number;
	ratingDocenza: number;
	ratingContenuti: number;
	commento: string;
}