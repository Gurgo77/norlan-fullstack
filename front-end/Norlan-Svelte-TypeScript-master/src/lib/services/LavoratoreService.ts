import httpClient from '$lib/api/httpClient';

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

	static async getByAzienda(idAzienda: number | string): Promise<DipendenteDTO[]> {
		const response = await httpClient.get<DipendenteDTO[]>(`${this.basePath}/azienda/${idAzienda}`);
		return response.data;
	}

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

	static async getDpiByLavoratore(idLavoratore: number | string): Promise<AssegnazioneDPIDTO[]> {
		const response = await httpClient.get<AssegnazioneDPIDTO[]>(
			`${this.basePath}/${idLavoratore}/dpi`
		);
		return response.data;
	}

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
