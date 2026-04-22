// src/lib/services/DocumentoService.ts
import httpClient from '../api/httpClient';
import { Documento, type DocumentoData } from '../models/Documento';
import { RichiestaRinnovo, type RichiestaRinnovoData } from '../models/RichiestaRinnovo';
import type { RichiestaRinnovoRequest } from '../models/RichiestaRinnovoRequest';

export class DocumentoService {
	private static readonly endpoint = '/api/documenti';

	static async getDocumentiByAzienda(idAzienda: number): Promise<Documento[]> {
		const response = await httpClient.get<DocumentoData[]>(`${this.endpoint}/azienda/${idAzienda}`);
		return response.data.map((item: DocumentoData) => new Documento(item));
	}

	static async getAllRinnovi(): Promise<RichiestaRinnovo[]> {
		const response = await httpClient.get<RichiestaRinnovoData[]>(`${this.endpoint}/rinnovi`);
		return response.data.map((item: RichiestaRinnovoData) => new RichiestaRinnovo(item));
	}

	static async downloadDocumento(id: number): Promise<Blob> {
		const response = await httpClient.get<Blob>(`${this.endpoint}/${id}/download`, {
			responseType: 'blob'
		});
		return response.data;
	}

	/**
	 * Carica un nuovo documento tramite FormData (Multipart).
	 * Endpoint: POST /api/documenti/azienda/{idAzienda}/upload
	 */
	static async uploadDocumento(idAzienda: number, formData: FormData): Promise<Documento> {
		const response = await httpClient.post<DocumentoData>(
			`${this.endpoint}/azienda/${idAzienda}/upload`,
			formData,
			{ headers: { 'Content-Type': 'multipart/form-data' } }
		);
		return new Documento(response.data);
	}

	/**
	 * Crea una nuova richiesta di rinnovo per un documento esistente.
	 * Endpoint: POST /api/documenti/{idDocumento}/rinnovi
	 */
	static async richiediRinnovo(idDocumento: number, dati: RichiestaRinnovoRequest): Promise<void> {
		await httpClient.post(`${this.endpoint}/${idDocumento}/rinnovi`, dati);
	}

	/**
	 * Cambia lo stato del documento in IN_ATTESA_FIRMA.
	 * Endpoint: PATCH /api/documenti/{id}/richiedi-firma
	 */
	static async richiediFirma(id: number): Promise<void> {
		await httpClient.patch(`${this.endpoint}/${id}/richiedi-firma`);
	}

	/**
	 * Cambia lo stato del documento in APPROVATO.
	 * Endpoint: PATCH /api/documenti/{id}/approva
	 */
	static async approvaDocumento(id: number): Promise<void> {
		await httpClient.patch(`${this.endpoint}/${id}/approva`);
	}

	/**
	 * Archivia il documento (Stato ARCHIVIATO).
	 * Endpoint: PATCH /api/documenti/{id}/archivia
	 */
	static async archiviaDocumento(id: number): Promise<void> {
		await httpClient.patch(`${this.endpoint}/${id}/archivia`);
	}
}
