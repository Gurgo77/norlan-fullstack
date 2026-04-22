import httpClient from '$lib/api/httpClient';
import { Dipendente, type DipendenteData } from '$lib/models/Dipendente';
import { Ruolo } from '$lib/models/Enums';

export interface DipendenteCreateRequest {
	email: string;
	password?: string;
	nome: string;
	cognome: string;
	codiceFiscale: string;
	mansione?: string;
	idAzienda: number | string;
	ruolo: Ruolo;
}

export class DipendenteService {
	// CORRETTO: Corrisponde a @RequestMapping("/api/lavoratori")
	private static basePath = '/api/lavoratori';

	static async getAll(): Promise<Dipendente[]> {
		const response = await httpClient.get<DipendenteData[]>(this.basePath);
		return response.data.map((data) => new Dipendente(data));
	}

	static async getByAzienda(idAzienda: number | string): Promise<Dipendente[]> {
		// Corrisponde a @GetMapping("/azienda/{idAzienda}")
		const response = await httpClient.get<DipendenteData[]>(
			`${this.basePath}/azienda/${idAzienda}`
		);
		return response.data.map((data) => new Dipendente(data));
	}

	static async delete(id: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/${id}`);
	}
}
