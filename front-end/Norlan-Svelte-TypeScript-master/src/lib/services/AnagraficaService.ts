import httpClient from '$lib/api/httpClient';

// Payload per la registrazione unificata
export interface AuthRequestDTO {
	email: string;
	password?: string;
	ruolo: 'AZIENDA' | 'DOCENTE' | 'LAVORATORE' | 'ADMIN';
	ragioneSociale?: string;
	nome?: string;
	cognome?: string;
	partitaIva?: string;
}

/**
 * Interfacce basate sui metodi setter presenti nel controller BE
 */
export interface AziendaUpdate {
	ragioneSociale: string;
	partitaIva: string;
	email?: string;
}

export interface DocenteUpdate {
	specializzazioneTecnica: string;
	email?: string;
}

export interface AdminUpdate {
	nome?: string;
	cognome?: string;
	email?: string;
}

export class AnagraficaService {
	private static readonly basePath = '/api/anagrafica';

	// ==========================================
	// SEZIONE REGISTRAZIONE
	// ==========================================

	static async registraUtente(dati: AuthRequestDTO): Promise<string> {
		const response = await httpClient.post<string>(`${this.basePath}/registrazione`, dati);
		return response.data;
	}

	// ==========================================
	// SEZIONE AZIENDE
	// ==========================================

	static async getAllAziende(): Promise<unknown[]> {
		const response = await httpClient.get<unknown[]>(`${this.basePath}/aziende`);
		return response.data;
	}

	static async getAziendaById(idAzienda: number | string): Promise<unknown> {
		const response = await httpClient.get<unknown>(`${this.basePath}/aziende/${idAzienda}`);
		return response.data;
	}

	/**
	 * @PutMapping("/aziende/{id}") - Aggiorna ragioneSociale, partitaIva ed email
	 */
	static async updateAzienda(
		idAzienda: number | string,
		datiAggiornati: AziendaUpdate
	): Promise<unknown> {
		const response = await httpClient.put<unknown>(
			`${this.basePath}/aziende/${idAzienda}`,
			datiAggiornati
		);
		return response.data;
	}

	static async deleteAzienda(idAzienda: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/aziende/${idAzienda}`);
	}

	// ==========================================
	// SEZIONE DOCENTI
	// ==========================================

	static async getAllDocenti(): Promise<unknown[]> {
		const response = await httpClient.get<unknown[]>(`${this.basePath}/docenti`);
		return response.data;
	}

	static async getDocenteById(idDocente: number | string): Promise<unknown> {
		const response = await httpClient.get<unknown>(`${this.basePath}/docenti/${idDocente}`);
		return response.data;
	}

	/**
	 * @PutMapping("/docenti/{id}") - Aggiorna specializzazioneTecnica ed email
	 */
	static async updateDocente(
		idDocente: number | string,
		datiAggiornati: DocenteUpdate
	): Promise<unknown> {
		const response = await httpClient.put<unknown>(
			`${this.basePath}/docenti/${idDocente}`,
			datiAggiornati
		);
		return response.data;
	}

	static async deleteDocente(idDocente: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/docenti/${idDocente}`);
	}

}
