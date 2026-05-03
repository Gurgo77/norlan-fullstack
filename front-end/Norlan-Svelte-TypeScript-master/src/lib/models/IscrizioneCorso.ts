import type { Documento } from './Documento';
import type {StatoCorso} from "$lib/models/Enums";

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
}

export interface IscrizioneCorso {
	idUtente: number;
	idCorso: number;
	presenzaConfermata: boolean;
	idDocumento?: number;
	documentoAttestato?: Documento;
}

export class IscrizioneCorso {
	idUtente: number;
	idCorso: number;
	emailUtente: string;
	titoloCorso: string;
	dataOrarioCorso: string;
	presenzaConfermata: boolean;
	statoCorso?: string;

	constructor(data: IscrizioneData) {
		this.idUtente = data.idUtente;
		this.idCorso = data.idCorso;
		this.emailUtente = data.emailUtente;
		this.titoloCorso = data.titoloCorso;
		this.dataOrarioCorso = data.dataOrarioCorso;
		this.presenzaConfermata = data.presenzaConfermata;
		this.statoCorso = data.statoCorso;

		if (data.idDocumento !== undefined) {
			this.idDocumento = data.idDocumento;
		}
		if (data.documentoAttestato !== undefined) {
			this.documentoAttestato = data.documentoAttestato;
		}
	}
}