import httpClient from '$lib/api/httpClient';
import { Documento, type DocumentoData } from '$lib/models/Documento';
import { RichiestaRinnovo, type RichiestaRinnovoData } from '$lib/models/RichiestaRinnovo';
import type { RichiestaRinnovoRequest } from '$lib/models/RichiestaRinnovoRequest';
/*
Servizio per la gestione completa del ciclo di vita dei documenti e delle relative richieste di rinnovo.
Coordina upload, download, archiviazione e i flussi di approvazione documentale.
*/
export type StatoRinnovo = 'IN_ATTESA' | 'APPROVATA' | 'RIFIUTATA' | 'COMPLETATA' | string;

export class DocumentoService {
	private static readonly basePath = '/api/documenti';

	static async getAllDocumenti(): Promise<Documento[]> {
		const { data } = await httpClient.get<DocumentoData[]>(this.basePath);
		return data.map((item) => new Documento(item));
	}

	static async getDocumentoById(id: number | string): Promise<Documento> {
		const { data } = await httpClient.get<DocumentoData>(`${this.basePath}/${id}`);
		return new Documento(data);
	}

	static async getDocumentiByAzienda(idAzienda: number | string): Promise<Documento[]> {
		const { data } = await httpClient.get<DocumentoData[]>(`${this.basePath}/azienda/${idAzienda}`);
		return data.map((item) => new Documento(item));
	}

	// Recupera i documenti con scadenza imminente entro il numero di giorni specificato
	static async getDocumentiInScadenza(giorni: number = 30): Promise<Documento[]> {
		const { data } = await httpClient.get<DocumentoData[]>(`${this.basePath}/in-scadenza`, {
			params: { giorni }
		});
		return data.map((item) => new Documento(item));
	}

	static async downloadDocumento(id: number | string): Promise<Blob> {
		const { data } = await httpClient.get<Blob>(`${this.basePath}/${id}/download`, {
			responseType: 'blob'
		});
		return data;
	}

	// Esegue l'upload di un nuovo file associato a un'azienda tramite form multipart
	static async uploadDocumento(idAzienda: number | string, formData: FormData): Promise<Documento> {
		const { data } = await httpClient.post<DocumentoData>(
			`${this.basePath}/azienda/${idAzienda}/upload`,
			formData,
			{
				headers: { 'Content-Type': 'multipart/form-data' }
			}
		);
		return new Documento(data);
	}

	static async deleteDocumento(id: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/${id}`);
	}

	// Innesca il processo di firma per il documento selezionato
	static async richiediFirma(id: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/${id}/richiedi-firma`);
	}

	// Approva il documento e, opzionalmente, invia la versione controfirmata
	static async approvaDocumento(id: number | string, file?: File): Promise<void> {
		if (file) {
			const formData = new FormData();
			formData.append('file', file);

			await httpClient.patch(`${this.basePath}/${id}/approva`, formData, {
				headers: { 'Content-Type': 'multipart/form-data' }
			});
		} else {
			await httpClient.patch(`${this.basePath}/${id}/approva`);
		}
	}

	static async archiviaDocumento(id: number | string): Promise<void> {
		await httpClient.patch(`${this.basePath}/${id}/archivia`);
	}

	// Recupera l'elenco completo delle richieste di rinnovo documentale attive nel sistema
	static async getAllRinnovi(): Promise<RichiestaRinnovo[]> {
		const { data } = await httpClient.get<RichiestaRinnovoData[]>(`${this.basePath}/rinnovi`);
		return data.map((item) => new RichiestaRinnovo(item));
	}

	static async getRinnoviByStato(stato: StatoRinnovo): Promise<RichiestaRinnovo[]> {
		const { data } = await httpClient.get<RichiestaRinnovoData[]>(
			`${this.basePath}/rinnovi/stato/${stato}`
		);
		return data.map((item) => new RichiestaRinnovo(item));
	}

	// Invia una nuova richiesta di rinnovo per un documento specifico, includendo note opzionali
	static async richiediRinnovo(
		idDocumento: number | string,
		dati: RichiestaRinnovoRequest | null = null
	): Promise<RichiestaRinnovo> {
		const { data } = await httpClient.post<RichiestaRinnovoData>(
			`${this.basePath}/${idDocumento}/rinnovi`,
			dati || {}
		);
		return new RichiestaRinnovo(data);
	}

	// Aggiorna lo stato di avanzamento di una pratica di rinnovo specifica
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