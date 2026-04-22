import httpClient from '$lib/api/httpClient';
import { Azienda, type AziendaData } from '$lib/models/Azienda';
import type { DipendenteData } from '$lib/models/Dipendente'; // Importato per il Metodo 2
import type { Ruolo } from '$lib/models/Enums';

/**
 * Interfaccia per la creazione di un'azienda.
 */
export interface AziendaCreateRequest {
	email: string;
	password?: string;
	ragioneSociale: string;
	partitaIva: string;
	sedeLegale?: string;
	pec?: string;
	telefono?: string;
	cellulare?: string;
	referenteAziendale?: string;
	hasDipendenti: boolean;
	ruolo: Ruolo;
}

export class AziendaService {
	private static readonly basePath = '/api/aziende';

	/**
	 * Recupera tutte le aziende
	 */
	static async getAll(): Promise<Azienda[]> {
		const response = await httpClient.get<AziendaData[]>(this.basePath);
		return response.data.map((data) => new Azienda(data));
	}

	/**
	 * Verifica dinamicamente se un'azienda ha dipendenti associati.
	 * Utilizza il tipo DipendenteData[] per evitare errori di linting (no-explicit-any).
	 * @param idAzienda L'ID dell'azienda da controllare
	 */
	static async hasDipendenti(idAzienda: number | string): Promise<boolean> {
		// Metodo 2: Tipizzazione esplicita con DipendenteData[]
		const response = await httpClient.get<DipendenteData[]>(`/api/dipendenti/azienda/${idAzienda}`);

		// Restituisce true se l'array contiene almeno un dipendente
		return response.data.length > 0;
	}

	/**
	 * Crea una nuova azienda
	 */
	static async create(payload: AziendaCreateRequest): Promise<Azienda> {
		const response = await httpClient.post<AziendaData>(this.basePath, payload);
		return new Azienda(response.data);
	}

	/**
	 * Elimina un'azienda tramite ID
	 */
	static async delete(idUtente: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/${idUtente}`);
	}
}
