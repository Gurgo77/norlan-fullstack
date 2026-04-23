import httpClient from '$lib/api/httpClient';

// ==========================================
// INTERFACCE PAYLOAD E DTO
// ==========================================

// Interfaccia basata sui campi estratti nel PUT e necessari per il POST
export interface DipendenteRequest {
	nome: string;
	cognome: string;
	codiceFiscale: string;
	email?: string;
	password?: string; // Solo se necessaria per la creazione, altrimenti opzionale
}

// Interfaccia basata sull'entità AssegnazioneDPI del Backend
export interface AssegnazioneDPIRequest {
	nomeDpi: string; // Sostituisci con i nomi esatti della tua entità Java
	dataConsegna: string; // Formato ISO: 'YYYY-MM-DD'
	dataScadenza?: string;
	note?: string;
}

// Interfacce di ritorno (Puoi importarle da $lib/models se le hai già create)
export interface DipendenteDTO {
	idUtente: number;
	nome: string;
	cognome: string;
	codiceFiscale: string;
	email: string;
	ruolo: string;
}

export interface AssegnazioneDPIDTO {
	id: number;
	nomeDpi: string;
	dataConsegna: string;
	dataScadenza: string;
	note: string;
}

export class LavoratoreService {
	// Allineato con @RequestMapping("/api/lavoratori")
	private static readonly basePath = '/api/lavoratori';

	// ==========================================
	// SEZIONE DIPENDENTI / LAVORATORI
	// ==========================================

	/**
	 * Recupera tutti i dipendenti del sistema.
	 * BE: GET /api/lavoratori
	 */
	static async getAll(): Promise<DipendenteDTO[]> {
		const response = await httpClient.get<DipendenteDTO[]>(this.basePath);
		return response.data;
	}

	/**
	 * NUOVO: Recupera un dipendente specifico tramite ID.
	 * BE: GET /api/lavoratori/{id}
	 */
	static async getById(id: number | string): Promise<DipendenteDTO> {
		const response = await httpClient.get<DipendenteDTO>(`${this.basePath}/${id}`);
		return response.data;
	}

	/**
	 * Recupera i dipendenti di una specifica azienda.
	 * BE: GET /api/lavoratori/azienda/{idAzienda}
	 */
	static async getByAzienda(idAzienda: number | string): Promise<DipendenteDTO[]> {
		const response = await httpClient.get<DipendenteDTO[]>(`${this.basePath}/azienda/${idAzienda}`);
		return response.data;
	}

	/**
	 * NUOVO: Crea un nuovo dipendente per un'azienda specifica.
	 * BE: POST /api/lavoratori/azienda/{idAzienda}
	 */
	static async create(idAzienda: number | string, dati: DipendenteRequest): Promise<DipendenteDTO> {
		const response = await httpClient.post<DipendenteDTO>(
			`${this.basePath}/azienda/${idAzienda}`,
			dati
		);
		return response.data;
	}

	/**
	 * NUOVO: Aggiorna i dati anagrafici di un dipendente.
	 * BE: PUT /api/lavoratori/{id}
	 */
	static async update(
		id: number | string,
		datiAggiornati: DipendenteRequest
	): Promise<DipendenteDTO> {
		const response = await httpClient.put<DipendenteDTO>(`${this.basePath}/${id}`, datiAggiornati);
		return response.data;
	}

	/**
	 * Elimina un dipendente.
	 * BE: DELETE /api/lavoratori/{id}
	 */
	static async delete(idLavoratore: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/${idLavoratore}`);
	}

	// ==========================================
	// SEZIONE DPI
	// ==========================================

	/**
	 * NUOVO: Recupera tutti i DPI assegnati a un lavoratore.
	 * BE: GET /api/lavoratori/{idDipendente}/dpi
	 */
	static async getDpiByLavoratore(idLavoratore: number | string): Promise<AssegnazioneDPIDTO[]> {
		const response = await httpClient.get<AssegnazioneDPIDTO[]>(
			`${this.basePath}/${idLavoratore}/dpi`
		);
		return response.data;
	}

	/**
	 * Assegna un nuovo DPI a un lavoratore.
	 * BE: POST /api/lavoratori/{idDipendente}/dpi
	 */
	static async assegnaDpi(
		idLavoratore: number | string,
		payload: AssegnazioneDPIRequest
	): Promise<AssegnazioneDPIDTO> {
		const response = await httpClient.post<AssegnazioneDPIDTO>(
			`${this.basePath}/${idLavoratore}/dpi`,
			payload
		);
		return response.data;
	}

	/**
	 * NUOVO: Recupera tutti i DPI in scadenza entro un certo numero di giorni (default 30).
	 * BE: GET /api/lavoratori/dpi/in-scadenza?giorni={giorni}
	 */
	static async getDpiInScadenza(giorni: number = 30): Promise<AssegnazioneDPIDTO[]> {
		const response = await httpClient.get<AssegnazioneDPIDTO[]>(
			`${this.basePath}/dpi/in-scadenza`,
			{
				params: { giorni }
			}
		);
		return response.data;
	}

	/**
	 * NUOVO: Rimuove un'assegnazione DPI errata.
	 * BE: DELETE /api/lavoratori/dpi/{idDpi}
	 */
	static async deleteDpi(idDpi: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/dpi/${idDpi}`);
	}
}
