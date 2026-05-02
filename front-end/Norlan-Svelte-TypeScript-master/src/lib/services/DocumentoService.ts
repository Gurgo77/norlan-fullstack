import httpClient from '$lib/api/httpClient';
import { Documento, type DocumentoData } from '$lib/models/Documento';
import { RichiestaRinnovo, type RichiestaRinnovoData } from '$lib/models/RichiestaRinnovo';
import type { RichiestaRinnovoRequest } from '$lib/models/RichiestaRinnovoRequest';

export type StatoRinnovo = 'IN_ATTESA' | 'APPROVATA' | 'RIFIUTATA' | 'COMPLETATA' | string;

export class DocumentoService {
	private static readonly basePath = '/api/documenti';

	static async getAllDocumenti(): Promise<Documento[]> {
		const response = await httpClient.get<DocumentoData[]>(this.basePath);
		return response.data.map((item) => new Documento(item));
	}

	static async getDocumentoById(id: number | string): Promise<Documento> {
		const response = await httpClient.get<DocumentoData>(`${this.basePath}/${id}`);
		return new Documento(response.data);
	}

	static async getDocumentiByAzienda(idAzienda: number | string): Promise<Documento[]> {
		const response = await httpClient.get<DocumentoData[]>(`${this.basePath}/azienda/${idAzienda}`);
		return response.data.map((item) => new Documento(item));
	}

	static async getDocumentiInScadenza(giorni: number = 30): Promise<Documento[]> {
		const response = await httpClient.get<DocumentoData[]>(`${this.basePath}/in-scadenza`, {
			params: { giorni }
		});
		return response.data.map((item) => new Documento(item));
	}

	static async downloadDocumento(id: number | string): Promise<Blob> {
		const response = await httpClient.get<Blob>(`${this.basePath}/${id}/download`, {
			responseType: 'blob'
		});
		return response.data;
	}

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

	static async deleteDocumento(id: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/${id}`);
	}

	static async richiediFirma(id: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/${id}/richiedi-firma`);
	}

	static async approvaDocumento(id: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/${id}/approva`);
	}

	static async archiviaDocumento(id: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/${id}/archivia`);
	}

	static async getAllRinnovi(): Promise<RichiestaRinnovo[]> {
		const response = await httpClient.get<RichiestaRinnovoData[]>(`${this.basePath}/rinnovi`);
		return response.data.map((item) => new RichiestaRinnovo(item));
	}

	static async getRinnoviByStato(stato: StatoRinnovo): Promise<RichiestaRinnovo[]> {
		const response = await httpClient.get<RichiestaRinnovoData[]>(
			`${this.basePath}/rinnovi/stato/${stato}`
		);
		return response.data.map((item) => new RichiestaRinnovo(item));
	}

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

	static async updateStatoRinnovo(
		idRichiesta: number | string,
		nuovoStato: StatoRinnovo
	): Promise<void> {
		await httpClient.patch(`${this.basePath}/rinnovi/${idRichiesta}/stato`, null, {
			params: { nuovoStato }
		});
	}

	static async deleteRinnovo(idRichiesta: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/rinnovi/${idRichiesta}`);
	}
}
