// Assicurati che ci sia 'export' prima di interface e class
import type { Documento } from './Documento';

// Definiamo rigorosamente l'interfaccia in ingresso al costruttore
export interface IscrizioneData {
	idUtente: number;
	idCorso: number;
	emailUtente: string;
	titoloCorso: string;
	dataOrarioCorso: string;
	presenzaConfermata: boolean;
	// Campi opzionali per la FSM
	idDocumento?: number;
	documentoAttestato?: Documento;
}

// eslint-disable-next-line @typescript-eslint/no-unsafe-declaration-merging
export interface IscrizioneCorso {
	idUtente: number;
	idCorso: number;
	presenzaConfermata: boolean;

	// Relazioni strutturali per l'attestato
	idDocumento?: number;
	documentoAttestato?: Documento;
}

// eslint-disable-next-line @typescript-eslint/no-unsafe-declaration-merging
export class IscrizioneCorso {
	idUtente: number;
	idCorso: number;
	emailUtente: string;
	titoloCorso: string;
	dataOrarioCorso: string;
	presenzaConfermata: boolean;

	constructor(data: IscrizioneData) {
		this.idUtente = data.idUtente;
		this.idCorso = data.idCorso;
		this.emailUtente = data.emailUtente;
		this.titoloCorso = data.titoloCorso;
		this.dataOrarioCorso = data.dataOrarioCorso;
		this.presenzaConfermata = data.presenzaConfermata;

		// Mappatura condizionale: se l'API restituisce il documento (Fase VALIDATO), lo assegnamo
		if (data.idDocumento !== undefined) {
			this.idDocumento = data.idDocumento;
		}
		if (data.documentoAttestato !== undefined) {
			this.documentoAttestato = data.documentoAttestato;
		}
	}
}