import httpClient from '$lib/api/httpClient';

// Interfaccia per la richiesta di login basata su AuthRequestDTO
export interface LoginRequest {
	email: string;
	password: string;
}

// Interfaccia per la risposta basata su AuthResponseDTO del backend
export interface LoginResponse {
	token: string;
	idUtente: number;
	email: string;
	ruolo: 'AZIENDA' | 'DOCENTE' | 'LAVORATORE' | 'ADMIN' | string;
}

// Interfaccia per i dati utente salvati in sessione
export interface UserSession {
	idUtente: number;
	email: string;
	ruolo: string;
}

export class AuthService {
	// Allineato con @RequestMapping("/api/auth")
	private static readonly basePath = '/api/auth';

	/**
	 * Esegue il login, riceve il token dal backend e inizializza la sessione locale.
	 * BE: POST /api/auth/login
	 */
	static async login(credentials: LoginRequest): Promise<LoginResponse> {
		const response = await httpClient.post<LoginResponse>(`${this.basePath}/login`, credentials);
		const authData = response.data;

		// Salvataggio persistente dei dati
		this.setSession(authData);

		return authData;
	}

	/**
	 * Salva i dati dell'utente e il token nel localStorage.
	 */
	private static setSession(authData: LoginResponse): void {
		if (typeof window === 'undefined') return;

		localStorage.setItem('jwt_token', authData.token);
		localStorage.setItem('userId', authData.idUtente.toString());
		localStorage.setItem('userEmail', authData.email);
		localStorage.setItem('userRole', authData.ruolo);

		const session: UserSession = {
			idUtente: authData.idUtente,
			email: authData.email,
			ruolo: authData.ruolo
		};

		localStorage.setItem('currentUser', JSON.stringify(session));
	}

	/**
	 * Recupera l'utente attualmente loggato dal localStorage.
	 */
	static getSession(): UserSession | null {
		if (typeof window === 'undefined') return null;

		const userStr = localStorage.getItem('currentUser');
		return userStr ? (JSON.parse(userStr) as UserSession) : null;
	}

	/**
	 * Recupera il token JWT salvato.
	 */
	static getToken(): string | null {
		if (typeof window === 'undefined') return null;
		return localStorage.getItem('jwt_token');
	}

	/**
	 * Esegue il logout chiamando il backend e pulendo i dati locali.
	 * BE: POST /api/auth/logout
	 */
	static async logout(): Promise<void> {
		if (typeof window === 'undefined') return;

		try {
			// Notifica il backend del logout
			await httpClient.post(`${this.basePath}/logout`);
		} catch (error) {
			console.warn('Il server non ha risposto al logout, procedo con la pulizia locale.', error);
		} finally {
			// Pulizia definitiva del localStorage
			localStorage.removeItem('jwt_token');
			localStorage.removeItem('userId');
			localStorage.removeItem('userEmail');
			localStorage.removeItem('userRole');
			localStorage.removeItem('currentUser');

			// Redirect alla pagina di login per resettare lo stato dell'app
			window.location.href = '/login';
		}
	}
}
