// src/lib/models/FeedbackRequest.ts
export interface FeedbackRequest {
	idUtente: number;      // Obbligatorio per @NotNull nel backend
	idCorso: number;       // Obbligatorio per @NotNull nel backend
	ratingDocenza: number;   // Deve chiamarsi così per il backend
	ratingContenuti: number; // Deve chiamarsi così per il backend
	commento: string;        // Max 1000 caratteri come da backend
}