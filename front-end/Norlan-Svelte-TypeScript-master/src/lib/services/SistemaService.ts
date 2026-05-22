import httpClient from '$lib/api/httpClient';
import { Notifica, type NotificaData } from '$lib/models/Notifica';
import { LogSincronizzazione, type LogSincronizzazioneData } from '$lib/models/LogSincronizzazione';
/*
Servizio per la gestione delle funzionalità di sistema trasversali.
Coordina l'invio e la lettura delle notifiche utente e gestisce il monitoraggio tramite log di sincronizzazione.
*/
export interface CreateLogRequest {
	descrizioneEvento: string;
	esitoPositivo: boolean;
	noteTecniche?: string;
}

export class SistemaService {
	private static readonly basePath = '/api/sistema';

	static async getNotificheUtente(idUtente: number | string): Promise<Notifica[]> {
		const response = await httpClient.get<NotificaData[]>(
			`${this.basePath}/notifiche/utente/${idUtente}`
		);
		return response.data.map((item) => new Notifica(item));
	}

	// Recupera l'elenco delle notifiche ancora da visualizzare per l'utente specificato
	static async getNotificheNonLette(idUtente: number | string): Promise<Notifica[]> {
		const response = await httpClient.get<NotificaData[]>(
			`${this.basePath}/notifiche/utente/${idUtente}/non-lette`
		);
		return response.data.map((item) => new Notifica(item));
	}

	// Calcola il numero totale di notifiche non lette per aggiornare i badge sull'interfaccia
	static async countNotificheNonLette(idUtente: number | string): Promise<number> {
		const response = await httpClient.get(
			`${this.basePath}/notifiche/utente/${idUtente}/non-lette/count`
		);

		const count = Number(response.data);
		return isNaN(count) ? 0 : count;
	}

	// Aggiorna lo stato della notifica impostandola come letta nel database
	static async segnaLetta(idNotifica: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/notifiche/${idNotifica}/letta`);
	}

	static async deleteNotifica(idNotifica: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/notifiche/${idNotifica}`);
	}

	static async getAllLogs(): Promise<LogSincronizzazione[]> {
		const response = await httpClient.get<LogSincronizzazioneData[]>(`${this.basePath}/logs`);
		return response.data.map((item) => new LogSincronizzazione(item));
	}

	// Filtra ed estrae esclusivamente i log che segnalano esiti negativi o errori di sincronizzazione
	static async getErrorLogs(): Promise<LogSincronizzazione[]> {
		const response = await httpClient.get<LogSincronizzazioneData[]>(
			`${this.basePath}/logs/errori`
		);
		return response.data.map((item) => new LogSincronizzazione(item));
	}

	// Registra un nuovo evento di sistema per fini di tracciamento e audit
	static async createLog(dati: CreateLogRequest): Promise<LogSincronizzazione> {
		const response = await httpClient.post<LogSincronizzazioneData>(`${this.basePath}/logs`, dati);
		return new LogSincronizzazione(response.data);
	}

	// Esegue la rimozione dei record di log che superano la soglia di anzianità definita
	static async pulisciLogVecchi(giorniVecchiaia: number = 30): Promise<void> {
		await httpClient.delete(`${this.basePath}/logs/pulizia`, {
			params: { giorniVecchiaia }
		});
	}
}
