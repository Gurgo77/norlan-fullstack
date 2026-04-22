import httpClient from '$lib/api/httpClient'; // Alias SvelteKit
import type { FeedbackRequest } from '$lib/models/FeedbackRequest';

export class FeedbackService {
	// Allineato con @RequestMapping("/api/feedback")
	private static readonly basePath = '/api/feedback';

	/**
	 * Invia il feedback di un utente/lavoratore al sistema.
	 * Ritorna il messaggio di successo generato dal backend.
	 * * Endpoint: POST /api/feedback/invia
	 */
	static async inviaFeedback(dati: FeedbackRequest): Promise<string> {
		// Il backend restituisce una stringa (es. "Feedback archiviato con successo...")
		const response = await httpClient.post<string>(`${this.basePath}/invia`, dati);
		return response.data;
	}
}
