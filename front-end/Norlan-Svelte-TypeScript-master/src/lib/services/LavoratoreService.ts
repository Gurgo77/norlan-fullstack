import httpClient from '$lib/api/httpClient'; // Alias SvelteKit
import { Dipendente, type DipendenteData } from '$lib/models/Dipendente';
import { AssegnazioneDPI, type AssegnazioneDPIData } from '$lib/models/AssegnazioneDPI';
import type { CreateDipendenteRequest } from '$lib/models/CreateDipendenteRequest';

// Definiamo l'interfaccia per l'assegnazione dei DPI (addio "any"!)
export interface AssegnaDpiRequest {
	nomeDpi: string; // Sostituisci con il nome esatto che si aspetta Spring Boot
	dataAssegnazione: string; // Formato ISO es. "2026-04-22"
	dataScadenza?: string; // Opzionale
	note?: string; // Opzionale
}

export class LavoratoreService {
	// Allineato con @RequestMapping("/api/lavoratori")
	private static readonly basePath = '/api/lavoratori';

	// ==========================================
	// SEZIONE DIPENDENTI
	// ==========================================

	/**
	 * Recupera tutti i dipendenti registrati nell'intero sistema (Vista Admin).
	 * Endpoint: GET /api/lavoratori
	 */
	static async getAllLavoratori(): Promise<Dipendente[]> {
		const response = await httpClient.get<DipendenteData[]>(this.basePath);
		return response.data.map((item) => new Dipendente(item));
	}

	/**
	 * Recupera la lista dei dipendenti di un'azienda specifica.
	 * Endpoint: GET /api/lavoratori/azienda/{idAzienda}
	 */
	static async getDipendentiByAzienda(idAzienda: number | string): Promise<Dipendente[]> {
		const response = await httpClient.get<DipendenteData[]>(
			`${this.basePath}/azienda/${idAzienda}`
		);
		return response.data.map((item) => new Dipendente(item));
	}

	/**
	 * Recupera i dettagli di un singolo dipendente.
	 * Endpoint: GET /api/lavoratori/{id}
	 */
	static async getDipendenteById(id: number | string): Promise<Dipendente> {
		const response = await httpClient.get<DipendenteData>(`${this.basePath}/${id}`);
		return new Dipendente(response.data);
	}

	/**
	 * Crea un nuovo dipendente associato a un'azienda.
	 * Rimosso l'any: ora accetta rigorosamente solo CreateDipendenteRequest
	 * Endpoint: POST /api/lavoratori/azienda/{idAzienda}
	 */
	static async createDipendente(
		idAzienda: number | string,
		dati: CreateDipendenteRequest
	): Promise<Dipendente> {
		const response = await httpClient.post<DipendenteData>(
			`${this.basePath}/azienda/${idAzienda}`,
			dati
		);
		return new Dipendente(response.data);
	}

	/**
	 * Elimina un dipendente dal sistema.
	 * Endpoint: DELETE /api/lavoratori/{id}
	 */
	static async deleteDipendente(id: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/${id}`);
	}

	// ==========================================
	// SEZIONE DPI (Dispositivi Protezione Individuale)
	// ==========================================

	/**
	 * Recupera i DPI assegnati a un singolo dipendente.
	 * Endpoint: GET /api/lavoratori/{idDipendente}/dpi
	 */
	static async getDpiByDipendente(idDipendente: number | string): Promise<AssegnazioneDPI[]> {
		const response = await httpClient.get<AssegnazioneDPIData[]>(
			`${this.basePath}/${idDipendente}/dpi`
		);
		return response.data.map((item) => new AssegnazioneDPI(item));
	}

	/**
	 * Assegna un nuovo DPI a un dipendente.
	 * Sostituito "any" con la nostra nuova interfaccia "AssegnaDpiRequest".
	 * Endpoint: POST /api/lavoratori/{idDipendente}/dpi
	 */
	static async assegnaDpi(
		idDipendente: number | string,
		datiAssegnazione: AssegnaDpiRequest
	): Promise<AssegnazioneDPI> {
		const response = await httpClient.post<AssegnazioneDPIData>(
			`${this.basePath}/${idDipendente}/dpi`,
			datiAssegnazione
		);
		return new AssegnazioneDPI(response.data);
	}

	/**
	 * Recupera tutti i DPI in scadenza entro un certo numero di giorni (default 30).
	 * Endpoint: GET /api/lavoratori/dpi/in-scadenza
	 */
	static async getDpiInScadenza(giorni: number = 30): Promise<AssegnazioneDPI[]> {
		const response = await httpClient.get<AssegnazioneDPIData[]>(
			`${this.basePath}/dpi/in-scadenza`,
			{
				params: { giorni }
			}
		);
		return response.data.map((item) => new AssegnazioneDPI(item));
	}

	/**
	 * Rimuove o invalida un'assegnazione DPI errata.
	 * Endpoint: DELETE /api/lavoratori/dpi/{idDpi}
	 */
	static async deleteDpi(idDpi: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/dpi/${idDpi}`);
	}
}
