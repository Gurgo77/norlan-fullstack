import httpClient from '../api/httpClient';
import { Dipendente, type DipendenteData } from '../models/Dipendente';
import { AssegnazioneDPI, type AssegnazioneDPIData } from '../models/AssegnazioneDPI';
import type { CreateDipendenteRequest } from '../models/CreateDipendenteRequest';

export class LavoratoreService {
	private static readonly endpoint = '/api/lavoratori';

	/**
	 * Recupera la lista dei dipendenti di un'azienda specifica.
	 * Endpoint: GET /api/lavoratori/azienda/{idAzienda}
	 */
	static async getDipendentiByAzienda(idAzienda: number): Promise<Dipendente[]> {
		// Tipizziamo la risposta con <DipendenteData[]> per eliminare l'errore 'any'
		const response = await httpClient.get<DipendenteData[]>(
			`${this.endpoint}/azienda/${idAzienda}`
		);
		return response.data.map((item: DipendenteData) => new Dipendente(item));
	}

	/**
	 * Crea un nuovo dipendente associato a un'azienda.
	 * Endpoint: POST /api/lavoratori/azienda/{idAzienda}
	 */
	static async createDipendente(
		idAzienda: number,
		dati: CreateDipendenteRequest
	): Promise<Dipendente> {
		const response = await httpClient.post<DipendenteData>(
			`${this.endpoint}/azienda/${idAzienda}`,
			dati
		);
		return new Dipendente(response.data);
	}

	/**
	 * Recupera i DPI assegnati a un singolo dipendente.
	 */
	static async getDpiByDipendente(idDipendente: number): Promise<AssegnazioneDPI[]> {
		const response = await httpClient.get<AssegnazioneDPIData[]>(
			`${this.endpoint}/${idDipendente}/dpi`
		);
		return response.data.map((item: AssegnazioneDPIData) => new AssegnazioneDPI(item));
	}

	/**
	 * Recupera tutti i DPI in scadenza entro un certo numero di giorni (default 30).
	 */
	static async getDpiInScadenza(giorni: number = 30): Promise<AssegnazioneDPI[]> {
		const response = await httpClient.get<AssegnazioneDPIData[]>(
			`${this.endpoint}/dpi/in-scadenza`,
			{
				params: { giorni }
			}
		);
		return response.data.map((item: AssegnazioneDPIData) => new AssegnazioneDPI(item));
	}
}
