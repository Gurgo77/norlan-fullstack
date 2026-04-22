import httpClient from '$lib/api/httpClient';
import { Notifica, type NotificaData } from '$lib/models/Notifica';
import { LogSincronizzazione, type LogSincronizzazioneData } from '$lib/models/LogSincronizzazione';

// Definiamo l'interfaccia esatta che il backend si aspetta per creare un log
export interface CreateLogRequest {
	descrizioneEvento: string;
	esitoPositivo: boolean;
	noteTecniche?: string; // Opzionale
}

export class SistemaService {
	// Allineato con @RequestMapping("/api/sistema")
	private static readonly basePath = '/api/sistema';

	// ==========================================
	// SEZIONE NOTIFICHE UTENTE
	// ==========================================

	/**
	 * Recupera tutte le notifiche di un determinato utente (Lavoratore o Admin).
	 * Endpoint: GET /api/sistema/notifiche/utente/{idUtente}
	 */
	static async getNotificheUtente(idUtente: number | string): Promise<Notifica[]> {
		const response = await httpClient.get<NotificaData[]>(
			`${this.basePath}/notifiche/utente/${idUtente}`
		);
		return response.data.map((item) => new Notifica(item));
	}

	/**
	 * Segna una notifica come letta.
	 * Endpoint: PATCH /api/sistema/notifiche/{idNotifica}/letta
	 */
	static async segnaLetta(idNotifica: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/notifiche/${idNotifica}/letta`);
	}

	// ==========================================
	// SEZIONE LOG DI SINCRONIZZAZIONE (Admin)
	// ==========================================

	/**
	 * Recupera la cronologia completa di tutti i log.
	 * Endpoint: GET /api/sistema/logs
	 */
	static async getAllLogs(): Promise<LogSincronizzazione[]> {
		const response = await httpClient.get<LogSincronizzazioneData[]>(`${this.basePath}/logs`);
		return response.data.map((item) => new LogSincronizzazione(item));
	}

	/**
	 * Recupera solo i log che contengono errori (Esito Positivo = false).
	 * Endpoint: GET /api/sistema/logs/errori
	 */
	static async getErrorLogs(): Promise<LogSincronizzazione[]> {
		const response = await httpClient.get<LogSincronizzazioneData[]>(
			`${this.basePath}/logs/errori`
		);
		return response.data.map((item) => new LogSincronizzazione(item));
	}

	/**
	 * Registra manualmente un evento di sistema.
	 * Eliminato l'errore "any" usando l'interfaccia CreateLogRequest.
	 * Endpoint: POST /api/sistema/logs
	 */
	static async createLog(dati: CreateLogRequest): Promise<LogSincronizzazione> {
		const response = await httpClient.post<LogSincronizzazioneData>(`${this.basePath}/logs`, dati);
		return new LogSincronizzazione(response.data);
	}

	/**
	 * Pulisce il database eliminando i log più vecchi di X giorni.
	 * Endpoint: DELETE /api/sistema/logs/pulizia
	 */
	static async pulisciLogVecchi(giorniVecchiaia: number = 30): Promise<void> {
		await httpClient.delete(`${this.basePath}/logs/pulizia`, {
			params: { giorniVecchiaia } // Inviato come query param ?giorniVecchiaia=30
		});
	}
}
