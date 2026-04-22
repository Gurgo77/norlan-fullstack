import { TipoDPI } from './Enums';

export interface AssegnazioneDPIData {
	idAssegnazione: number;
	idDipendente: number;
	tipo: TipoDPI;
	dataConsegna: string;
	dataScadenzaRevisione: string;
	daRevisionare: boolean;
}

export class AssegnazioneDPI {
	idAssegnazione: number;
	idDipendente: number;
	tipo: TipoDPI;
	dataConsegna: string;
	dataScadenzaRevisione: string;
	daRevisionare: boolean;

	constructor(data: AssegnazioneDPIData) {
		this.idAssegnazione = data.idAssegnazione;
		this.idDipendente = data.idDipendente;
		this.tipo = data.tipo;
		this.dataConsegna = data.dataConsegna;
		this.dataScadenzaRevisione = data.dataScadenzaRevisione;
		this.daRevisionare = data.daRevisionare;
	}
}
