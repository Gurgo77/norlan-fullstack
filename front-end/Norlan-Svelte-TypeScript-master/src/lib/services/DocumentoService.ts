import httpClient from '$lib/api/httpClient';
import { Documento, type DocumentoData } from '$lib/models/Documento';
import { RichiestaRinnovo, type RichiestaRinnovoData } from '$lib/models/RichiestaRinnovo';
import type { RichiestaRinnovoRequest } from '$lib/models/RichiestaRinnovoRequest';

export class DocumentoService {
	// Allineato con @RequestMapping("/api/documenti")
	private static readonly basePath = '/api/documenti';

	static async getDocumentiByAzienda(idAzienda: number | string): Promise<Documento[]> {
		const response = await httpClient.get<DocumentoData[]>(`${this.basePath}/azienda/${idAzienda}`);
		return response.data.map((item) => new Documento(item));
	}

	static async getAllRinnovi(): Promise<RichiestaRinnovo[]> {
		const response = await httpClient.get<RichiestaRinnovoData[]>(`${this.basePath}/rinnovi`);
		return response.data.map((item) => new RichiestaRinnovo(item));
	}

	static async downloadDocumento(id: number | string): Promise<Blob> {
		const response = await httpClient.get<Blob>(`${this.basePath}/${id}/download`, {
			responseType: 'blob' // Fondamentale per ricevere il file come file binario PDF
		});
		return response.data;
	}

	/**
	 * Carica un nuovo documento tramite FormData (Multipart).
	 * Endpoint: POST /api/documenti/azienda/{idAzienda}/upload
	 */
	static async uploadDocumento(idAzienda: number | string, formData: FormData): Promise<Documento> {
		const response = await httpClient.post<DocumentoData>(
			`${this.basePath}/azienda/${idAzienda}/upload`,
			formData,
			{ headers: { 'Content-Type': 'multipart/form-data' } }
		);
		return new Documento(response.data);
	}

	// ==========================================
	// SEZIONE RINNOVI
	// ==========================================

	/**
	 * Crea una nuova richiesta di rinnovo per un documento esistente.
	 * Endpoint: POST /api/documenti/{idDocumento}/rinnovi
	 */
	static async richiediRinnovo(
		idDocumento: number | string,
		dati: RichiestaRinnovoRequest
	): Promise<RichiestaRinnovo> {
		// Aggiunto il ritorno del dato: il backend Java restituisce HttpStatus.CREATED con il DTO
		const response = await httpClient.post<RichiestaRinnovoData>(
			`${this.basePath}/${idDocumento}/rinnovi`,
			dati
		);
		return new RichiestaRinnovo(response.data);
	}

	/**
	 * NUOVO: Cambia lo stato della richiesta di rinnovo (es. da ATTESA a COMPLETATA).
	 * Endpoint: PATCH /api/documenti/rinnovi/{idRichiesta}/stato
	 */
	static async updateStatoRinnovo(idRichiesta: number | string, nuovoStato: string): Promise<void> {
		await httpClient.patch(`${this.basePath}/rinnovi/${idRichiesta}/stato`, null, {
			params: { nuovoStato } // Passiamo il nuovoStato come RequestParam come richiede il controller
		});
	}

	/**
	 * NUOVO: Elimina una richiesta di rinnovo.
	 * Endpoint: DELETE /api/documenti/rinnovi/{idRichiesta}
	 */
	static async deleteRinnovo(idRichiesta: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/rinnovi/${idRichiesta}`);
	}

	// ==========================================
	// SEZIONE STATI DOCUMENTO
	// ==========================================

	/**
	 * Cambia lo stato del documento in IN_ATTESA_FIRMA.
	 * Endpoint: PATCH /api/documenti/{id}/richiedi-firma
	 */
	static async richiediFirma(id: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/${id}/richiedi-firma`);
	}

	/**
	 * Cambia lo stato del documento in APPROVATO.
	 * Endpoint: PATCH /api/documenti/{id}/approva
	 */
	static async approvaDocumento(id: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/${id}/approva`);
	}

	/**
	 * Archivia il documento (Stato ARCHIVIATO).
	 * Endpoint: PATCH /api/documenti/{id}/archivia
	 */
	static async archiviaDocumento(id: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/${id}/archivia`);
	}
}
