import httpClient from '$lib/api/httpClient';
import { CorsoFormazione, type CorsoData } from '$lib/models/CorsoFormazione';
import { IscrizioneCorso, type IscrizioneData } from '$lib/models/IscrizioneCorso';
import { MaterialeDidattico, type MaterialeDidatticoData } from '$lib/models/MaterialeDidattico';
import type { CorsoFormazioneRequest } from '$lib/models/CorsoFormazioneRequest';

// Tipo helper per lo stato del corso, utile per evitare di sbagliare stringhe
export type StatoCorso = 'PROGRAMMATO' | 'IN_CORSO' | 'COMPLETATO' | 'ANNULLATO' | string;

export class FormazioneService {
	// Allineato con @RequestMapping("/api/formazione") nel Backend
	private static readonly basePath = '/api/formazione';

	// ==========================================
	// SEZIONE CORSI E FSM (MACCHINA A STATI)
	// ==========================================

	/**
	 * Valida le presenze di un corso (Passaggio da CONCLUSO a ATTESA_FIRMA_DOCENTE)
	 * BE: POST /api/formazione/corsi/{idCorso}/valida-presenze
	 */
	static async validaPresenzeAdmin(idCorso: number, idUtentiPresenti: number[]): Promise<void> {
		await httpClient.post(`${this.basePath}/corsi/${idCorso}/valida-presenze`, idUtentiPresenti);
	}

	/**
	 * Il docente firma il registro (Passaggio da ATTESA_FIRMA_DOCENTE a VALIDATO)
	 * BE: POST /api/formazione/corsi/{idCorso}/firma-docente
	 */
	static async controfirmaRegistro(idCorso: number): Promise<void> {
		await httpClient.post(`${this.basePath}/corsi/${idCorso}/firma-docente`);
	}

	/**
	 * Genera e distribuisce gli attestati alle aziende (Da VALIDATO in poi)
	 * BE: POST /api/formazione/corsi/{idCorso}/distribuisci-attestati
	 */
	/**
	 * Carica e distribuisce i PDF separati per le singole aziende
	 * BE: POST /api/formazione/corsi/{idCorso}/distribuisci-attestati
	 */
	static async distribuisciAttestati(
		idCorso: number,
		payload: { file: File; idAzienda: number }[]
	): Promise<void> {
		const formData = new FormData();
		payload.forEach(item => {
			formData.append('files', item.file);
			formData.append('idAziende', item.idAzienda.toString());
		});

		// Sovrascriviamo l'interceptor globale forzando il multipart
		await httpClient.post(`${this.basePath}/corsi/${idCorso}/distribuisci-attestati`, formData, {
			headers: {
				'Content-Type': 'multipart/form-data'
			}
		});
	}

	// ==========================================
	// SEZIONE CRUD CORSI
	// ==========================================

	/**
	 * Recupera tutti i corsi.
	 * BE: GET /api/formazione/corsi
	 */
	static async getAllCorsi(): Promise<CorsoFormazione[]> {
		const response = await httpClient.get<CorsoData[]>(`${this.basePath}/corsi`);
		return response.data.map((item) => new CorsoFormazione(item));
	}

	/**
	 * Recupera un singolo corso tramite ID.
	 * BE: GET /api/formazione/corsi/{idCorso}
	 */
	static async getCorsoById(id: number | string): Promise<CorsoFormazione> {
		const response = await httpClient.get<CorsoData>(`${this.basePath}/corsi/${id}`);
		return new CorsoFormazione(response.data);
	}

	/**
	 * Recupera i corsi filtrati per stato.
	 * BE: GET /api/formazione/corsi/stato/{stato}
	 */
	static async getCorsiByStato(stato: StatoCorso): Promise<CorsoFormazione[]> {
		const response = await httpClient.get<CorsoData[]>(`${this.basePath}/corsi/stato/${stato}`);
		return response.data.map((item) => new CorsoFormazione(item));
	}

	/**
	 * Crea un nuovo corso.
	 * BE: POST /api/formazione/corsi
	 */
	static async createCorso(dati: CorsoFormazioneRequest): Promise<CorsoFormazione> {
		const response = await httpClient.post<CorsoData>(`${this.basePath}/corsi`, dati);
		return new CorsoFormazione(response.data);
	}

	/**
	 * Aggiorna lo stato di un corso.
	 * BE: PATCH /api/formazione/corsi/{idCorso}/stato
	 */
	static async updateStatoCorso(idCorso: number | string, nuovoStato: StatoCorso): Promise<void> {
		await httpClient.patch(`${this.basePath}/corsi/${idCorso}/stato`, null, {
			params: { nuovoStato }
		});
	}

	/**
	 * Elimina un corso.
	 * BE: DELETE /api/formazione/corsi/{idCorso}
	 */
	static async deleteCorso(idCorso: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/corsi/${idCorso}`);
	}

	// ==========================================
	// SEZIONE ISCRIZIONI
	// ==========================================

	/**
	 * Recupera le iscrizioni di un utente (lavoratore).
	 * BE: GET /api/formazione/iscrizioni/utente/{idUtente}
	 */
	static async getIscrizioniUtente(idUtente: number | string): Promise<IscrizioneCorso[]> {
		const response = await httpClient.get<IscrizioneData[]>(
			`${this.basePath}/iscrizioni/utente/${idUtente}`
		);
		return response.data.map((item) => new IscrizioneCorso(item));
	}

	/**
	 * Recupera tutti gli iscritti a un determinato corso.
	 * BE: GET /api/formazione/corsi/{idCorso}/iscrizioni
	 */
	static async getIscrizioniByCorso(idCorso: number | string): Promise<IscrizioneCorso[]> {
		const response = await httpClient.get<IscrizioneData[]>(
			`${this.basePath}/corsi/${idCorso}/iscrizioni`
		);
		return response.data.map((item) => new IscrizioneCorso(item));
	}

	/**
	 * Iscrive un lavoratore a un corso.
	 * BE: POST /api/formazione/corsi/{idCorso}/iscrizioni/{idUtente}
	 */
	static async iscriviUtente(
		idCorso: number | string,
		idUtente: number | string
	): Promise<IscrizioneCorso> {
		const response = await httpClient.post<IscrizioneData>(
			`${this.basePath}/corsi/${idCorso}/iscrizioni/${idUtente}`
		);
		return new IscrizioneCorso(response.data);
	}

	/**
	 * Valida la presenza di un lavoratore al corso.
	 * BE: PATCH /api/formazione/corsi/{idCorso}/iscrizioni/{idLavoratore}/presenza
	 */
	static async validaPresenza(
		idCorso: number | string,
		idLavoratore: number | string
	): Promise<void> {
		await httpClient.patch(`${this.basePath}/corsi/${idCorso}/iscrizioni/${idLavoratore}/presenza`);
	}

	/**
	 * Sblocca/Genera il certificato per un lavoratore.
	 * ATTENZIONE: Aggiunto il parametro pathFile richiesto dal BE.
	 * BE: PATCH /api/formazione/corsi/{idCorso}/iscrizioni/{idLavoratore}/certificato
	 */
	static async uploadAttestato(idCorso: number | string, idLavoratore: number | string, file: File): Promise<string> {
		const formData = new FormData();
		formData.append('file', file);

		const response = await httpClient.post(`${this.basePath}/corsi/${idCorso}/iscrizioni/${idLavoratore}/certificato`, formData, {
			headers: {
				'Content-Type': 'multipart/form-data'
			}
		});
		return response.data;
	}

	/**
	 * Scarica l'attestato di un lavoratore
	 * BE: GET /api/formazione/corsi/{idCorso}/iscrizioni/{idUtente}/certificato/download
	 */
	static async downloadAttestato(idCorso: number | string, idUtente: number | string): Promise<Blob> {
		const response = await httpClient.get<Blob>(
			`${this.basePath}/corsi/${idCorso}/iscrizioni/${idUtente}/certificato/download`,
			{ responseType: 'blob' }
		);
		return response.data;
	}

	/**
	 * Rimuove un'iscrizione a un corso.
	 * BE: DELETE /api/formazione/corsi/{idCorso}/iscrizioni/{idUtente}
	 */
	static async rimuoviIscrizione(
		idCorso: number | string,
		idUtente: number | string
	): Promise<void> {
		await httpClient.delete(`${this.basePath}/corsi/${idCorso}/iscrizioni/${idUtente}`);
	}

	// ==========================================
	// SEZIONE MATERIALI DIDATTICI
	// ==========================================

	/**
	 * Recupera i materiali associati a un corso.
	 * BE: GET /api/formazione/corsi/{idCorso}/materiali
	 */
	static async getMaterialiByCorso(idCorso: number | string): Promise<MaterialeDidattico[]> {
		const response = await httpClient.get<MaterialeDidatticoData[]>(
			`${this.basePath}/corsi/${idCorso}/materiali`
		);
		return response.data.map((item) => new MaterialeDidattico(item));
	}

	/**
	 * Carica un materiale didattico per un corso.
	 * ATTENZIONE: Il formData deve contenere "file" e "titoloDocumento" come chiavi.
	 * BE: POST /api/formazione/corsi/{idCorso}/materiali
	 */
	static async uploadMateriale(
		idCorso: number | string,
		formData: FormData
	): Promise<MaterialeDidattico> {
		const response = await httpClient.post<MaterialeDidatticoData>(
			`${this.basePath}/corsi/${idCorso}/materiali`,
			formData,
			{ headers: { 'Content-Type': 'multipart/form-data' } }
		);
		return new MaterialeDidattico(response.data);
	}

	/**
	 * Scarica un materiale didattico (File PDF, ecc.).
	 * BE: GET /api/formazione/materiali/{idMateriale}/download
	 */
	static async downloadMateriale(idMateriale: number | string): Promise<Blob> {
		const response = await httpClient.get<Blob>(
			`${this.basePath}/materiali/${idMateriale}/download`,
			{ responseType: 'blob' }
		);
		return response.data;
	}

	/**
	 * Elimina un materiale didattico.
	 * BE: DELETE /api/formazione/materiali/{idMateriale}
	 */
	static async deleteMateriale(idMateriale: number | string): Promise<void> {
		await httpClient.delete(`${this.basePath}/materiali/${idMateriale}`);
	}
}