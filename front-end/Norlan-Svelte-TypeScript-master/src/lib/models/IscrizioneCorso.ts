import type { Documento } from './Documento';
import type { StatoCorso } from '$lib/models/Enums';

export interface IscrizioneData {
	idUtente: number;
	idCorso: number;
	emailUtente: string;
	titoloCorso: string;
	dataOrarioCorso: string;
	presenzaConfermata: boolean;
	idDocumento?: number;
	documentoAttestato?: Documento;
	statoCorso?: StatoCorso;
	feedbackInviato?: boolean;
}

export class IscrizioneCorso {
	idUtente: number;
	idCorso: number;
	emailUtente: string;
	titoloCorso: string;
	dataOrarioCorso: string;
	presenzaConfermata: boolean;
	statoCorso?: string;
	idDocumento?: number; // <-- Aggiunta qui
	documentoAttestato?: Documento; // <-- Aggiunta qui
	feedbackInviato?: boolean;

	constructor(data: IscrizioneData) {
		this.idUtente = data.idUtente;
		this.idCorso = data.idCorso;
		this.emailUtente = data.emailUtente;
		this.titoloCorso = data.titoloCorso;
		this.dataOrarioCorso = data.dataOrarioCorso;
		this.presenzaConfermata = data.presenzaConfermata;
		this.statoCorso = data.statoCorso;
		this.feedbackInviato = data.feedbackInviato;

		if (data.idDocumento !== undefined) {
			this.idDocumento = data.idDocumento;
		}
		if (data.documentoAttestato !== undefined) {
			this.documentoAttestato = data.documentoAttestato;
		}
	}
}
