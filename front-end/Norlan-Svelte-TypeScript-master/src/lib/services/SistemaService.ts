import httpClient from '$lib/api/httpClient';
import { Notifica, type NotificaData } from '$lib/models/Notifica';
import { LogSincronizzazione, type LogSincronizzazioneData } from '$lib/models/LogSincronizzazione';

export interface CreateLogRequest {
	descrizioneEvento: string;
	esitoPositivo: boolean;
	noteTecniche?: string; // Opzionale
}

export class SistemaService {
	private static readonly basePath = '/api/sistema';

	static async getNotificheUtente(idUtente: number | string): Promise<Notifica[]> {
		const response = await httpClient.get<NotificaData[]>(
			`${this.basePath}/notifiche/utente/${idUtente}`
		);
		return response.data.map((item) => new Notifica(item));
	}

	static async getNotificheNonLette(idUtente: number | string): Promise<Notifica[]> {
		const response = await httpClient.get<NotificaData[]>(
			`${this.basePath}/notifiche/utente/${idUtente}/non-lette`
		);
		return response.data.map((item) => new Notifica(item));
	}

	static async countNotificheNonLette(idUtente: number | string): Promise<number> {
		const response = await httpClient.get(
			`${this.basePath}/notifiche/utente/${idUtente}/non-lette/count`
		);

		const count = Number(response.data);
		return isNaN(count) ? 0 : count;
	}

	static async segnaLetta(idNotifica: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/notifiche/${idNotifica}/letta`);
	}

	/**
	 * Elimina una notifica letta/vecchia.
	 * BE: DELETE /api/sistema/notifiche/{idNotifica}
	 */
	static async deleteNotifica(idNotifica: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/notifiche/${idNotifica}`);
	}

	static async getAllLogs(): Promise<LogSincronizzazione[]> {
		const response = await httpClient.get<LogSincronizzazioneData[]>(`${this.basePath}/logs`);
		return response.data.map((item) => new LogSincronizzazione(item));
	}

	static async getErrorLogs(): Promise<LogSincronizzazione[]> {
		const response = await httpClient.get<LogSincronizzazioneData[]>(
			`${this.basePath}/logs/errori`
		);
		return response.data.map((item) => new LogSincronizzazione(item));
	}

	static async createLog(dati: CreateLogRequest): Promise<LogSincronizzazione> {
		const response = await httpClient.post<LogSincronizzazioneData>(`${this.basePath}/logs`, dati);
		return new LogSincronizzazione(response.data);
	}

	static async pulisciLogVecchi(giorniVecchiaia: number = 30): Promise<void> {
		await httpClient.delete(`${this.basePath}/logs/pulizia`, {
			params: { giorniVecchiaia } // Inviato come query param ?giorniVecchiaia=30
		});
	}
}