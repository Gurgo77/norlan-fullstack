import httpClient from '$lib/api/httpClient';
/*
Servizio per la gestione dei lavoratori (dipendenti) e delle dotazioni DPI associate.
Coordina le operazioni CRUD sulle anagrafiche e gestisce il ciclo di vita delle assegnazioni dei dispositivi.
*/
export interface DipendenteRequest {
	nome: string;
	cognome: string;
	codiceFiscale: string;
	email?: string;
	password?: string;
}

export interface AssegnazioneDPIRequest {
	nomeDpi: string;
	dataConsegna: string;
	dataScadenza?: string;
	note?: string;
}

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
	private static readonly basePath = '/api/lavoratori';

	static async getAll(): Promise<DipendenteDTO[]> {
		const response = await httpClient.get<DipendenteDTO[]>(this.basePath);
		return response.data;
	}

	static async getById(id: number | string): Promise<DipendenteDTO> {
		const response = await httpClient.get<DipendenteDTO>(`${this.basePath}/${id}`);
		return response.data;
	}

	// Recupera l'elenco dei lavoratori appartenenti a una specifica azienda
	static async getByAzienda(idAzienda: number | string): Promise<DipendenteDTO[]> {
		const response = await httpClient.get<DipendenteDTO[]>(`${this.basePath}/azienda/${idAzienda}`);
		return response.data;
	}

	// Registra un nuovo lavoratore nel sistema, associandolo all'azienda di riferimento
	static async create(idAzienda: number | string, dati: DipendenteRequest): Promise<DipendenteDTO> {
		const response = await httpClient.post<DipendenteDTO>(
			`${this.basePath}/azienda/${idAzienda}`,
			dati
		);
		return response.data;
	}

	static async update(
		id: number | string,
		datiAggiornati: DipendenteRequest
	): Promise<DipendenteDTO> {
		const response = await httpClient.put<DipendenteDTO>(`${this.basePath}/${id}`, datiAggiornati);
		return response.data;
	}

	static async delete(idLavoratore: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/${idLavoratore}`);
	}

	// Recupera lo storico dei dispositivi DPI assegnati a uno specifico lavoratore
	static async getDpiByLavoratore(idLavoratore: number | string): Promise<AssegnazioneDPIDTO[]> {
		const response = await httpClient.get<AssegnazioneDPIDTO[]>(
			`${this.basePath}/${idLavoratore}/dpi`
		);
		return response.data;
	}

	// Registra l'assegnazione di un nuovo DPI a un lavoratore, definendo date di consegna e scadenza
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

	// Filtra i DPI assegnati che raggiungeranno la data di revisione entro il numero di giorni indicato
	static async getDpiInScadenza(giorni: number = 30): Promise<AssegnazioneDPIDTO[]> {
		const response = await httpClient.get<AssegnazioneDPIDTO[]>(
			`${this.basePath}/dpi/in-scadenza`,
			{
				params: { giorni }
			}
		);
		return response.data;
	}

	static async deleteDpi(idDpi: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/dpi/${idDpi}`);
	}
}
