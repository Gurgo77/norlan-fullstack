import { TipoDPI } from './Enums';
/*
Modello per la gestione delle assegnazioni dei Dispositivi di Protezione Individuale (DPI).
Traccia le consegne ai dipendenti, le relative scadenze e lo stato delle revisioni.
*/
export interface AssegnazioneDPIData {
	idAssegnazione: number;
	idDipendente: number;
	tipo: TipoDPI;
	dataConsegna: string;
	dataScadenzaRevisione: string;
	daRevisionare: boolean;
	nomeDpi?: string;
}

export class AssegnazioneDPI {
	idAssegnazione: number;
	idDipendente: number;
	tipo: TipoDPI;
	dataConsegna: string;
	dataScadenzaRevisione: string;
	daRevisionare: boolean;
	nomeDpi?: string;

	constructor(data: AssegnazioneDPIData) {
		this.idAssegnazione = data.idAssegnazione;
		this.idDipendente = data.idDipendente;
		this.tipo = data.tipo;
		this.dataConsegna = data.dataConsegna;
		this.dataScadenzaRevisione = data.dataScadenzaRevisione;
		this.daRevisionare = data.daRevisionare;
		this.nomeDpi = data.nomeDpi;
	}
}
