export interface AuthResponseData {
	token: string;
	idUtente: number;
	email: string;
	ruolo: string;
}

export class AuthResponse {
	token: string;
	idUtente: number;
	email: string;
	ruolo: string;

	constructor(data: AuthResponseData) {
		this.token = data.token;
		this.idUtente = data.idUtente;
		this.email = data.email;
		this.ruolo = data.ruolo;
	}
}
