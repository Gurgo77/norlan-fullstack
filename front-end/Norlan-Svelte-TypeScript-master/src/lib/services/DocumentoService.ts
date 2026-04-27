import httpClient from '$lib/api/httpClient';
import { Documento, type DocumentoData } from '$lib/models/Documento';
import { RichiestaRinnovo, type RichiestaRinnovoData } from '$lib/models/RichiestaRinnovo';
import type { RichiestaRinnovoRequest } from '$lib/models/RichiestaRinnovoRequest';

// Tipo helper per lo stato del rinnovo, per evitare stringhe generiche
export type StatoRinnovo = 'IN_ATTESA' | 'APPROVATA' | 'RIFIUTATA' | 'COMPLETATA' | string;

export class DocumentoService {
	// Allineato con @RequestMapping("/api/documenti")
	private static readonly basePath = '/api/documenti';

	// ==========================================
	// SEZIONE RECUPERO DOCUMENTI
	// ==========================================

	/**
	 * Recupera tutti i documenti.
	 * BE: GET /api/documenti
	 */
	static async getAllDocumenti(): Promise<Documento[]> {
		const response = await httpClient.get<DocumentoData[]>(this.basePath);
		return response.data.map((item) => new Documento(item));
	}

	/**
	 * Recupera un documento specifico tramite ID.
	 * BE: GET /api/documenti/{id}
	 */
	static async getDocumentoById(id: number | string): Promise<Documento> {
		const response = await httpClient.get<DocumentoData>(`${this.basePath}/${id}`);
		return new Documento(response.data);
	}

	/**
	 * Recupera tutti i documenti associati a una specifica azienda.
	 * BE: GET /api/documenti/azienda/{idAzienda}
	 */
	static async getDocumentiByAzienda(idAzienda: number | string): Promise<Documento[]> {
		const response = await httpClient.get<DocumentoData[]>(`${this.basePath}/azienda/${idAzienda}`);
		return response.data.map((item) => new Documento(item));
	}

	/**
	 * Recupera i documenti in scadenza entro un certo numero di giorni (default 30).
	 * BE: GET /api/documenti/in-scadenza?giorni={giorni}
	 */
	static async getDocumentiInScadenza(giorni: number = 30): Promise<Documento[]> {
		const response = await httpClient.get<DocumentoData[]>(`${this.basePath}/in-scadenza`, {
			params: { giorni }
		});
		return response.data.map((item) => new Documento(item));
	}

	// ==========================================
	// SEZIONE GESTIONE FILE (UPLOAD/DOWNLOAD/DELETE)
	// ==========================================

	/**
	 * Scarica il file fisico (PDF/ecc.) del documento.
	 * BE: GET /api/documenti/{id}/download
	 */
	static async downloadDocumento(id: number | string): Promise<Blob> {
		const response = await httpClient.get<Blob>(`${this.basePath}/${id}/download`, {
			responseType: 'blob'
		});
		return response.data;
	}

	/**
	 * Carica un nuovo documento.
	 * ATTENZIONE: Il FormData deve contenere i campi: 'file', 'modulo', 'tipologia', 'dataScadenzaStr'
	 * BE: POST /api/documenti/azienda/{idAzienda}/upload
	 */
	// Nel file src/lib/services/DocumentoService.ts



	static async uploadDocumento(idAzienda: number | string, formData: FormData): Promise<Documento> {
		const response = await httpClient.post<DocumentoData>(
			`${this.basePath}/azienda/${idAzienda}/upload`,
			formData,
			{
				headers: {
					// Sovrascriviamo l'impostazione globale 'application/json' del file httpClient.ts
					'Content-Type': 'multipart/form-data'
				}
			}
		);
		return new Documento(response.data);
	}

	/**
	 * Elimina un documento dal sistema.
	 * BE: DELETE /api/documenti/{id}
	 */
	static async deleteDocumento(id: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/${id}`);
	}

	// ==========================================
	// SEZIONE STATI DOCUMENTO
	// ==========================================

	/**
	 * Cambia lo stato del documento in IN_ATTESA_FIRMA.
	 * BE: PATCH /api/documenti/{id}/richiedi-firma
	 */
	static async richiediFirma(id: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/${id}/richiedi-firma`);
	}

	/**
	 * Cambia lo stato del documento in APPROVATO.
	 * BE: PATCH /api/documenti/{id}/approva
	 */
	static async approvaDocumento(id: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/${id}/approva`);
	}

	/**
	 * Archivia il documento (Stato ARCHIVIATO).
	 * BE: PATCH /api/documenti/{id}/archivia
	 */
	static async archiviaDocumento(id: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/${id}/archivia`);
	}

	// ==========================================
	// SEZIONE RINNOVI
	// ==========================================

	/**
	 * Recupera tutte le richieste di rinnovo.
	 * BE: GET /api/documenti/rinnovi
	 */
	static async getAllRinnovi(): Promise<RichiestaRinnovo[]> {
		const response = await httpClient.get<RichiestaRinnovoData[]>(`${this.basePath}/rinnovi`);
		return response.data.map((item) => new RichiestaRinnovo(item));
	}

	/**
	 * Recupera i rinnovi filtrati per uno specifico stato.
	 * BE: GET /api/documenti/rinnovi/stato/{stato}
	 */
	static async getRinnoviByStato(stato: StatoRinnovo): Promise<RichiestaRinnovo[]> {
		const response = await httpClient.get<RichiestaRinnovoData[]>(
			`${this.basePath}/rinnovi/stato/${stato}`
		);
		return response.data.map((item) => new RichiestaRinnovo(item));
	}

	/**
	 * Crea una nuova richiesta di rinnovo per un documento esistente.
	 * BE: POST /api/documenti/{idDocumento}/rinnovi
	 */
	static async richiediRinnovo(
		idDocumento: number | string,
		dati: RichiestaRinnovoRequest | null = null
	): Promise<RichiestaRinnovo> {
		// Nel BE il RequestBody è required = false, quindi passiamo un oggetto vuoto se null
		const response = await httpClient.post<RichiestaRinnovoData>(
			`${this.basePath}/${idDocumento}/rinnovi`,
			dati || {}
		);
		return new RichiestaRinnovo(response.data);
	}

	/**
	 * Cambia lo stato di una richiesta di rinnovo.
	 * BE: PATCH /api/documenti/rinnovi/{idRichiesta}/stato
	 */
	static async updateStatoRinnovo(
		idRichiesta: number | string,
		nuovoStato: StatoRinnovo
	): Promise<void> {
		await httpClient.patch(`${this.basePath}/rinnovi/${idRichiesta}/stato`, null, {
			params: { nuovoStato }
		});
	}

	/**
	 * Elimina una richiesta di rinnovo.
	 * BE: DELETE /api/documenti/rinnovi/{idRichiesta}
	 */
	static async deleteRinnovo(idRichiesta: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/rinnovi/${idRichiesta}`);
	}
}
