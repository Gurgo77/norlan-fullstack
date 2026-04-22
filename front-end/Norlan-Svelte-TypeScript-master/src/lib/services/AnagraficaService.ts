import httpClient from '../api/httpClient';
import { Azienda, type AziendaData } from '../models/Azienda';
import { Docente, type DocenteData } from '../models/Docente';
import type { AuthRequest } from '../models/AuthRequest';

export class AnagraficaService {
	private static readonly endpoint = '/api/anagrafica';

	/**
	 * Recupera la lista completa di tutte le aziende registrate.
	 * Endpoint: GET /api/anagrafica/aziende
	 */
	static async getAllAziende(): Promise<Azienda[]> {
		// Tipizziamo la risposta con <AziendaData[]> per eliminare l'errore 'any'
		const response = await httpClient.get<AziendaData[]>(`${this.endpoint}/aziende`);
		return response.data.map((item: AziendaData) => new Azienda(item));
	}

	/**
	 * Crea una nuova anagrafica Azienda (Registrazione).
	 * Usiamo AuthRequest perché contiene i campi necessari al Factory Method del backend.
	 * Endpoint: POST /api/anagrafica/aziende
	 */
	static async createAzienda(dati: AuthRequest): Promise<Azienda> {
		const response = await httpClient.post<AziendaData>(`${this.endpoint}/aziende`, dati);
		return new Azienda(response.data);
	}

	/**
	 * Recupera la lista di tutti i docenti qualificati.
	 * Endpoint: GET /api/anagrafica/docenti
	 */
	static async getAllDocenti(): Promise<Docente[]> {
		const response = await httpClient.get<DocenteData[]>(`${this.endpoint}/docenti`);
		return response.data.map((item: DocenteData) => new Docente(item));
	}
}
