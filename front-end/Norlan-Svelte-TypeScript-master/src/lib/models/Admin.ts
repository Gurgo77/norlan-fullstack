import { Ruolo } from './Enums';
/*
Modello di dominio per l'entità Amministratore.
Contiene l'interfaccia per il tipaggio dei dati grezzi (DTO)
e la classe concreta per l'istanziazione degli oggetti nel client.
*/
export interface AdminData {
	idUtente: number;
	email: string;
	ruolo: Ruolo;
}

export class Admin {
	idUtente: number;
	email: string;
	ruolo: Ruolo;

	constructor(data: AdminData) {
		this.idUtente = data.idUtente;
		this.email = data.email;
		this.ruolo = data.ruolo;
	}
}
