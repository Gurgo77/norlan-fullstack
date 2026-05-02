import httpClient from '$lib/api/httpClient'; // Alias SvelteKit
import type { FeedbackRequest } from '$lib/models/FeedbackRequest';

export class FeedbackService {
	private static readonly basePath = '/api/feedback';

	static async inviaFeedback(dati: FeedbackRequest): Promise<string> {
		const response = await httpClient.post<string>(`${this.basePath}/invia`, dati);
		return response.data;
	}
}
