/*
Modelli per la gestione del flusso di autenticazione e autorizzazione.
Definiscono il DTO per le credenziali di accesso e il payload di risposta contenente il token.
*/
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