import httpClient from '$lib/api/httpClient';
import { CorsoFormazione, type CorsoData } from '$lib/models/CorsoFormazione';
import { IscrizioneCorso, type IscrizioneData } from '$lib/models/IscrizioneCorso';
import { MaterialeDidattico, type MaterialeDidatticoData } from '$lib/models/MaterialeDidattico';
import type { CorsoFormazioneRequest } from '$lib/models/CorsoFormazioneRequest';

export type StatoCorso = 'PROGRAMMATO' | 'IN_CORSO' | 'COMPLETATO' | 'ANNULLATO' | string;

export class FormazioneService {
	private static readonly basePath = '/api/formazione';

	static async validaPresenzeAdmin(idCorso: number, idUtentiPresenti: number[]): Promise<void> {
		await httpClient.post(`${this.basePath}/corsi/${idCorso}/valida-presenze`, idUtentiPresenti);
	}

	static async controfirmaRegistro(idCorso: number): Promise<void> {
		await httpClient.post(`${this.basePath}/corsi/${idCorso}/firma-docente`);
	}

	static async distribuisciAttestati(
		idCorso: number,
		payload: { file: File; idAzienda: number }[]
	): Promise<void> {
		const formData = new FormData();
		payload.forEach(item => {
			formData.append('files', item.file);
			formData.append('idAziende', item.idAzienda.toString());
		});

		await httpClient.post(`${this.basePath}/corsi/${idCorso}/distribuisci-attestati`, formData, {
			headers: {
				'Content-Type': 'multipart/form-data'
			}
		});
	}

	static async getAllCorsi(): Promise<CorsoFormazione[]> {
		const response = await httpClient.get<CorsoData[]>(`${this.basePath}/corsi`);
		return response.data.map((item) => new CorsoFormazione(item));
	}

	static async getCorsoById(id: number | string): Promise<CorsoFormazione> {
		const response = await httpClient.get<CorsoData>(`${this.basePath}/corsi/${id}`);
		return new CorsoFormazione(response.data);
	}

	static async getCorsiByStato(stato: StatoCorso): Promise<CorsoFormazione[]> {
		const response = await httpClient.get<CorsoData[]>(`${this.basePath}/corsi/stato/${stato}`);
		return response.data.map((item) => new CorsoFormazione(item));
	}

	static async createCorso(dati: CorsoFormazioneRequest): Promise<CorsoFormazione> {
		const response = await httpClient.post<CorsoData>(`${this.basePath}/corsi`, dati);
		return new CorsoFormazione(response.data);
	}

	static async updateStatoCorso(idCorso: number | string, nuovoStato: StatoCorso): Promise<void> {
		await httpClient.patch(`${this.basePath}/corsi/${idCorso}/stato`, null, {
			params: { nuovoStato }
		});
	}

	static async deleteCorso(idCorso: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/corsi/${idCorso}`);
	}

	static async getIscrizioniUtente(idUtente: number | string): Promise<IscrizioneCorso[]> {
		const response = await httpClient.get<IscrizioneData[]>(
			`${this.basePath}/iscrizioni/utente/${idUtente}`
		);
		return response.data.map((item) => new IscrizioneCorso(item));
	}

	static async getIscrizioniByCorso(idCorso: number | string): Promise<IscrizioneCorso[]> {
		const response = await httpClient.get<IscrizioneData[]>(
			`${this.basePath}/corsi/${idCorso}/iscrizioni`
		);
		return response.data.map((item) => new IscrizioneCorso(item));
	}

	static async iscriviUtente(
		idCorso: number | string,
		idUtente: number | string
	): Promise<IscrizioneCorso> {
		const response = await httpClient.post<IscrizioneData>(
			`${this.basePath}/corsi/${idCorso}/iscrizioni/${idUtente}`
		);
		return new IscrizioneCorso(response.data);
	}

	static async validaPresenza(
		idCorso: number | string,
		idLavoratore: number | string
	): Promise<void> {
		await httpClient.patch(`${this.basePath}/corsi/${idCorso}/iscrizioni/${idLavoratore}/presenza`);
	}

	static async uploadAttestato(idCorso: number | string, idLavoratore: number | string, file: File): Promise<string> {
		const formData = new FormData();
		formData.append('file', file);

		const response = await httpClient.post(`${this.basePath}/corsi/${idCorso}/iscrizioni/${idLavoratore}/certificato`, formData, {
			headers: {
				'Content-Type': 'multipart/form-data'
			}
		});
		return response.data;
	}

	static async downloadAttestato(idCorso: number | string, idUtente: number | string): Promise<Blob> {
		const response = await httpClient.get<Blob>(
			`${this.basePath}/corsi/${idCorso}/iscrizioni/${idUtente}/certificato/download`,
			{ responseType: 'blob' }
		);
		return response.data;
	}

	static async rimuoviIscrizione(
		idCorso: number | string,
		idUtente: number | string
	): Promise<void> {
		await httpClient.delete(`${this.basePath}/corsi/${idCorso}/iscrizioni/${idUtente}`);
	}

	static async getMaterialiByCorso(idCorso: number | string): Promise<MaterialeDidattico[]> {
		const response = await httpClient.get<MaterialeDidatticoData[]>(
			`${this.basePath}/corsi/${idCorso}/materiali`
		);
		return response.data.map((item) => new MaterialeDidattico(item));
	}

	static async uploadMateriale(
		idCorso: number | string,
		formData: FormData
	): Promise<MaterialeDidattico> {
		const response = await httpClient.post<MaterialeDidatticoData>(
			`${this.basePath}/corsi/${idCorso}/materiali`,
			formData,
			{ headers: { 'Content-Type': 'multipart/form-data' } }
		);
		return new MaterialeDidattico(response.data);
	}

	static async downloadMateriale(idMateriale: number | string): Promise<Blob> {
		const response = await httpClient.get<Blob>(
			`${this.basePath}/materiali/${idMateriale}/download`,
			{ responseType: 'blob' }
		);
		return response.data;
	}

	static async deleteMateriale(idMateriale: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/materiali/${idMateriale}`);
	}
}