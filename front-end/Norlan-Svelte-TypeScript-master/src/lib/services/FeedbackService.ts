import httpClient from '$lib/api/httpClient';
import type { FeedbackRequest } from '$lib/models/FeedbackRequest';
import type {FeedbackStatsDTO} from "$lib/models/Feedback";
/*
Servizio per la gestione dei feedback sui corsi di formazione.
Permette l'invio delle valutazioni da parte degli utenti e il recupero delle statistiche aggregate per i docenti.
*/
export class FeedbackService {
	private static readonly basePath = '/api/feedback';

	// Invia la valutazione del corso espressa dall'utente (rating e commento) al server
	static async inviaFeedback(dati: FeedbackRequest): Promise<string> {
		const response = await httpClient.post<string>(`${this.basePath}/invia`, dati);
		return response.data;
	}

	static async getStatisticheCorso(idCorso: number): Promise<FeedbackStatsDTO> {
		const response = await httpClient.get<FeedbackStatsDTO>(`${this.basePath}/corso/${idCorso}`);
		return response.data;
	}
}
