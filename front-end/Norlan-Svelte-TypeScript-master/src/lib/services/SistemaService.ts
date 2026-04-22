// src/lib/services/SistemaService.ts
import httpClient from '../api/httpClient';
import { Notifica, type NotificaData } from '../models/Notifica';

export class SistemaService {
	private static readonly endpoint = '/api/sistema';

	static async getNotificheUtente(idUtente: number): Promise<Notifica[]> {
		const response = await httpClient.get<NotificaData[]>(
			`${this.endpoint}/notifiche/utente/${idUtente}`
		);
		return response.data.map((item: NotificaData) => new Notifica(item));
	}

	static async segnaLetta(idNotifica: number): Promise<void> {
		await httpClient.patch(`${this.endpoint}/notifiche/${idNotifica}/letta`);
	}
}
