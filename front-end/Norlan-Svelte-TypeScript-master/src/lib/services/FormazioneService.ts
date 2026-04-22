import httpClient from '../api/httpClient';
import { CorsoFormazione, type CorsoData } from '../models/CorsoFormazione';
import { IscrizioneCorso, type IscrizioneData } from '../models/IscrizioneCorso';
import type { CorsoFormazioneRequest } from '../models/CorsoFormazioneRequest';

export class FormazioneService {
	private static readonly endpoint = '/api/formazione';

	/**
	 * Recupera tutti i corsi di formazione disponibili nel sistema.
	 * Endpoint: GET /api/formazione/corsi
	 */
	static async getAllCorsi(): Promise<CorsoFormazione[]> {
		// Tipizziamo la risposta con <CorsoData[]> per soddisfare ESLint
		const response = await httpClient.get<CorsoData[]>(`${this.endpoint}/corsi`);
		return response.data.map((item: CorsoData) => new CorsoFormazione(item));
	}

	/**
	 * Crea un nuovo corso di formazione (Azione riservata all'Admin).
	 * Endpoint: POST /api/formazione/corsi
	 */
	static async createCorso(dati: CorsoFormazioneRequest): Promise<CorsoFormazione> {
		const response = await httpClient.post<CorsoData>(`${this.endpoint}/corsi`, dati);
		return new CorsoFormazione(response.data);
	}

	/**
	 * Iscrive un utente (dipendente) a un corso specifico.
	 * Endpoint: POST /api/formazione/corsi/{idCorso}/iscrizioni/{idUtente}
	 */
	static async iscriviUtente(idCorso: number, idUtente: number): Promise<IscrizioneCorso> {
		const response = await httpClient.post<IscrizioneData>(
			`${this.endpoint}/corsi/${idCorso}/iscrizioni/${idUtente}`
		);
		return new IscrizioneCorso(response.data);
	}
}
