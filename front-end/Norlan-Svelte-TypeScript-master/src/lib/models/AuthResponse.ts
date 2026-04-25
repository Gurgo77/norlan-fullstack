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
	richiedeCambioPassword: boolean; // Aggiunto qui per renderlo accessibile nell'app

	constructor(data: AuthResponseData) {
		this.token = data.token;
		this.idUtente = data.idUtente;
		this.email = data.email;
		this.ruolo = data.ruolo;
		// Mappiamo il dato dall'interfaccia alla proprietà della classe
		this.richiedeCambioPassword = data.richiedeCambioPassword ?? false;
	}
}