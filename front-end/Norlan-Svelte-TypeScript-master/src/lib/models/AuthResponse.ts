export interface AuthResponseData {
	token: string;
	idUtente: number;
	email: string;
	ruolo: string;
	richiedeCambioPassword: boolean;
}

export class AuthResponse {
	token: string;
	idUtente: number;
	email: string;
	ruolo: string;
	richiedeCambioPassword: boolean;

	constructor(data: AuthResponseData) {
		this.token = data.token;
		this.idUtente = data.idUtente;
		this.email = data.email;
		this.ruolo = data.ruolo;
		this.richiedeCambioPassword = data.richiedeCambioPassword ?? false;
	}
}