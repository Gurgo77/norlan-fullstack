import { Ruolo } from './Enums';

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
