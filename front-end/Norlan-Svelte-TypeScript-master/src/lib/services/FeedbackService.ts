// src/lib/services/FeedbackService.ts
import httpClient from '../api/httpClient';
import type { FeedbackRequest } from '../models/FeedbackRequest';

export class FeedbackService {
	private static readonly endpoint = '/api/feedback';

	/**
	 * Invia il feedback di un lavoratore per un corso specifico.
	 * Endpoint: POST /api/feedback/invia
	 */
	static async inviaFeedback(dati: FeedbackRequest): Promise<void> {
		await httpClient.post(`${this.endpoint}/invia`, dati);
	}
}
