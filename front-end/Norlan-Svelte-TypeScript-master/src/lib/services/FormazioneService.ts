import httpClient from '$lib/api/httpClient';
import { CorsoFormazione, type CorsoData } from '$lib/models/CorsoFormazione';
import { IscrizioneCorso, type IscrizioneData } from '$lib/models/IscrizioneCorso';
import { MaterialeDidattico, type MaterialeDidatticoData } from '$lib/models/MaterialeDidattico';
import type { CorsoFormazioneRequest } from '$lib/models/CorsoFormazioneRequest';

export class FormazioneService {
	// Allineato con @RequestMapping("/api/formazione")
	private static readonly basePath = '/api/formazione';

	// ==========================================
	// SEZIONE CORSI
	// ==========================================

	/**
	 * Recupera tutti i corsi di formazione.
	 * Endpoint: GET /api/formazione
	 */
	static async getAllCorsi(): Promise<CorsoFormazione[]> {
		const response = await httpClient.get<CorsoData[]>(this.basePath);
		return response.data.map((item) => new CorsoFormazione(item));
	}

	/**
	 * Recupera un singolo corso tramite ID.
	 * Endpoint: GET /api/formazione/{id}
	 */
	static async getCorsoById(id: number | string): Promise<CorsoFormazione> {
		const response = await httpClient.get<CorsoData>(`${this.basePath}/${id}`);
		return new CorsoFormazione(response.data);
	}

	/**
	 * Crea un nuovo corso (Admin).
	 * Rimosso "any" per mantenere la tipizzazione stretta.
	 * Endpoint: POST /api/formazione
	 */
	static async createCorso(dati: CorsoFormazioneRequest): Promise<CorsoFormazione> {
		const response = await httpClient.post<CorsoData>(this.basePath, dati);
		return new CorsoFormazione(response.data);
	}

	// ==========================================
	// SEZIONE ISCRIZIONI
	// ==========================================

	/**
	 * Iscrive un lavoratore a un corso.
	 * Endpoint: POST /api/formazione/{id}/iscrizioni/{idUtente}
	 */
	static async iscriviUtente(
		idCorso: number | string,
		idUtente: number | string
	): Promise<IscrizioneCorso> {
		const response = await httpClient.post<IscrizioneData>(
			`${this.basePath}/${idCorso}/iscrizioni/${idUtente}`
		);
		return new IscrizioneCorso(response.data);
	}

	/**
	 * Recupera tutte le iscrizioni di un specifico lavoratore.
	 * Endpoint: GET /api/formazione/iscrizioni/lavoratore/{idUtente}
	 */
	static async getIscrizioniLavoratore(idUtente: number | string): Promise<IscrizioneCorso[]> {
		const response = await httpClient.get<IscrizioneData[]>(
			`${this.basePath}/iscrizioni/lavoratore/${idUtente}`
		);
		return response.data.map((item) => new IscrizioneCorso(item));
	}

	/**
	 * Aggiorna lo stato di un'iscrizione (es. da ISCRITTO a COMPLETATO).
	 * Endpoint: PATCH /api/formazione/iscrizioni/{idIscrizione}/stato
	 */
	static async updateStatoIscrizione(
		idIscrizione: number | string,
		nuovoStato: string
	): Promise<void> {
		await httpClient.patch(`${this.basePath}/iscrizioni/${idIscrizione}/stato`, null, {
			params: { nuovoStato } // Passato come @RequestParam
		});
	}

	// ==========================================
	// SEZIONE MATERIALI DIDATTICI
	// ==========================================

	/**
	 * Carica un file (PDF) come materiale per un corso.
	 * Endpoint: POST /api/formazione/{idCorso}/materiali/upload
	 */
	static async uploadMateriale(
		idCorso: number | string,
		formData: FormData
	): Promise<MaterialeDidattico> {
		const response = await httpClient.post<MaterialeDidatticoData>(
			`${this.basePath}/${idCorso}/materiali/upload`,
			formData,
			{ headers: { 'Content-Type': 'multipart/form-data' } }
		);
		return new MaterialeDidattico(response.data);
	}

	/**
	 * Scarica un materiale didattico.
	 */
	static async downloadMateriale(idMateriale: number | string): Promise<Blob> {
		const response = await httpClient.get<Blob>(
			`${this.basePath}/materiali/${idMateriale}/download`,
			{
				responseType: 'blob'
			}
		);
		return response.data;
	}

	/**
	 * Elimina un materiale didattico.
	 */
	static async deleteMateriale(idMateriale: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/materiali/${idMateriale}`);
	}
}
