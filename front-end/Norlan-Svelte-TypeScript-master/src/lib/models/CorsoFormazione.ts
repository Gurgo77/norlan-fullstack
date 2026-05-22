import { StatoCorso } from './Enums';
/*
Modello di dominio per i corsi di formazione.
Definisce i dettagli logistici del corso, lo stato attuale e i riferimenti al docente.
*/
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
