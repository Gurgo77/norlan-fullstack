import httpClient from '$lib/api/httpClient';
import type { DipendenteData } from '$lib/models/Dipendente';
import type { AziendaData } from '$lib/models/Azienda';
import type { AdminData } from '$lib/models/Admin';

export interface AuthRequestDTO {
	email: string;
	password?: string;
	ruolo: 'AZIENDA' | 'DOCENTE' | 'LAVORATORE' | 'ADMIN';
	ragioneSociale?: string;
	nome?: string;
	cognome?: string;
	partitaIva?: string;
}

export interface AziendaUpdate {
	ragioneSociale: string;
	partitaIva: string;
	email?: string;
	sedeLegale?: string;
	pec?: string;
	telefono?: string;
	cellulare?: string;
	referenteAziendale?: string;
}

export interface DocenteUpdate {
	specializzazioneTecnica: string;
	email?: string;
}

export interface AdminUpdate {
	email: string;
}

export class AnagraficaService {
	private static readonly basePath = '/api/anagrafica';

	static async registraUtente(dati: AuthRequestDTO): Promise<string> {
		const response = await httpClient.post<string>(`${this.basePath}/registrazione`, dati);
		return response.data;
	}

	static async getAllAziende(): Promise<unknown[]> {
		const response = await httpClient.get<unknown[]>(`${this.basePath}/aziende`);
		return response.data;
	}

	static async getAziendaById(idAzienda: number | string): Promise<AziendaData> {
		const response = await httpClient.get<AziendaData>(`${this.basePath}/aziende/${idAzienda}`);
		return response.data;
	}

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

	static async hasDipendenti(idAzienda: number | string): Promise<boolean> {
		const response = await httpClient.get<DipendenteData[]>(`/api/lavoratori/azienda/${idAzienda}`);

		return response.data.length > 0;
	}

	static async deleteAzienda(idAzienda: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/aziende/${idAzienda}`);
	}

	static async getAllDocenti(): Promise<unknown[]> {
		const response = await httpClient.get<unknown[]>(`${this.basePath}/docenti`);
		return response.data;
	}

	static async getDocenteById(idDocente: number | string): Promise<unknown> {
		const response = await httpClient.get<unknown>(`${this.basePath}/docenti/${idDocente}`);
		return response.data;
	}

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

	static async getAdminById(id: number | string): Promise<AdminData> {
		const response = await httpClient.get<AdminData>(`${this.basePath}/admin/${id}`);
		return response.data;
	}

	static async updateAdmin(id: number | string, dati: AdminUpdate): Promise<void> {
		await httpClient.put(`${this.basePath}/admin/${id}`, dati);
	}

	static async getAllDipendenti(): Promise<DipendenteData[]> {
		const response = await httpClient.get<DipendenteData[]>(`${this.basePath}/dipendenti`);
		return response.data;
	}
}
