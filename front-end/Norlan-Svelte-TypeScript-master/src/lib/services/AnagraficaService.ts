import httpClient from '$lib/api/httpClient';
import { Azienda, type AziendaData } from '$lib/models/Azienda';
import { Docente, type DocenteData } from '$lib/models/Docente';
import type { Ruolo } from '$lib/models/Enums';

// --- INTERFACCE DI RICHIESTA (Addio Any!) ---

export interface CreateAziendaRequest {
	ragioneSociale: string;
	partitaIva: string;
	email: string;
	password: string;
	sedeLegale?: string;
	pec?: string;
	telefono?: string;
	cellulare?: string;
	referenteAziendale?: string;
	hasDipendenti: boolean;
	ruolo: Ruolo;
}

export interface CreateDocenteRequest {
	nome: string;
	cognome: string;
	email: string;
	password: string;
	codiceFiscale: string;
	specializzazione?: string;
	telefono?: string;
	ruolo: Ruolo;
}

// --- SERVICE ---

export class AnagraficaService {
	private static readonly basePath = '/api/anagrafica';

	// ==========================================
	// SEZIONE AZIENDE
	// ==========================================

	/**
	 * Recupera tutte le aziende registrate.
	 */
	static async getAllAziende(): Promise<Azienda[]> {
		const response = await httpClient.get<AziendaData[]>(`${this.basePath}/aziende`);
		return response.data.map((item) => new Azienda(item));
	}

	/**
	 * Crea una nuova azienda con validazione dei tipi.
	 */
	static async createAzienda(dati: CreateAziendaRequest): Promise<Azienda> {
		const response = await httpClient.post<AziendaData>(`${this.basePath}/aziende`, dati);
		return new Azienda(response.data);
	}

	/**
	 * Elimina un'azienda tramite ID.
	 */
	static async deleteAzienda(id: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/aziende/${id}`);
	}

	// ==========================================
	// SEZIONE DOCENTI
	// ==========================================

	/**
	 * Recupera tutti i docenti registrati.
	 */
	static async getAllDocenti(): Promise<Docente[]> {
		const response = await httpClient.get<DocenteData[]>(`${this.basePath}/docenti`);
		return response.data.map((item) => new Docente(item));
	}

	/**
	 * Crea un nuovo docente con validazione dei tipi.
	 */
	static async createDocente(dati: CreateDocenteRequest): Promise<Docente> {
		const response = await httpClient.post<DocenteData>(`${this.basePath}/docenti`, dati);
		return new Docente(response.data);
	}

	/**
	 * Elimina un docente tramite ID.
	 */
	static async deleteDocente(id: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/docenti/${id}`);
	}
}
