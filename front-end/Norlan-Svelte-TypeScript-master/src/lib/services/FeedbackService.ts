import httpClient from '$lib/api/httpClient';
import type { FeedbackRequest } from '$lib/models/FeedbackRequest';
import type {FeedbackStatsDTO} from "$lib/models/Feedback";

export class FeedbackService {
	private static readonly basePath = '/api/feedback';

	static async inviaFeedback(dati: FeedbackRequest): Promise<string> {
		const response = await httpClient.post<string>(`${this.basePath}/invia`, dati);
		return response.data;
	}

	static async getStatisticheCorso(idCorso: number): Promise<FeedbackStatsDTO> {
		const response = await httpClient.get<FeedbackStatsDTO>(`${this.basePath}/corso/${idCorso}`);
		return response.data;
	}
}
