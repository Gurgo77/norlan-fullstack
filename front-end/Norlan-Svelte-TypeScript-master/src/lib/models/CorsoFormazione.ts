import { StatoCorso } from './Enums';

export interface CorsoData {
	idCorso: number;
	titolo: string;
	dataOrario: string;
	luogoFisico: string;
	capacitaMassima: number;
	stato: StatoCorso;
	idDocente: number;
	emailDocente: string;
}

export class CorsoFormazione {
	idCorso: number;
	titolo: string;
	dataOrario: string;
	luogoFisico: string;
	capacitaMassima: number;
	stato: StatoCorso;
	idDocente: number;
	emailDocente: string;

	constructor(data: CorsoData) {
		this.idCorso = data.idCorso;
		this.titolo = data.titolo;
		this.dataOrario = data.dataOrario;
		this.luogoFisico = data.luogoFisico;
		this.capacitaMassima = data.capacitaMassima;
		this.stato = data.stato;
		this.idDocente = data.idDocente;
		this.emailDocente = data.emailDocente;
	}
}
