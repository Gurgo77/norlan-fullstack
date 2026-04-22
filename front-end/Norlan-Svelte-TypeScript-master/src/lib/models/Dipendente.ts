import { Ruolo } from './Enums';

export interface DipendenteData {
	idUtente: number;
	nome: string;
	cognome: string;
	email: string;
	ruolo: Ruolo;
	codiceFiscale: string;
	idAzienda: number;
	ragioneSocialeAzienda: string;
}

export class Dipendente {
	idUtente: number;
	nome: string;
	cognome: string;
	email: string;
	ruolo: Ruolo;
	codiceFiscale: string;
	idAzienda: number;
	ragioneSocialeAzienda: string;

	constructor(data: DipendenteData) {
		this.idUtente = data.idUtente;
		this.nome = data.nome;
		this.cognome = data.cognome;
		this.email = data.email;
		this.ruolo = data.ruolo;
		this.codiceFiscale = data.codiceFiscale;
		this.idAzienda = data.idAzienda;
		this.ragioneSocialeAzienda = data.ragioneSocialeAzienda;
	}
}
