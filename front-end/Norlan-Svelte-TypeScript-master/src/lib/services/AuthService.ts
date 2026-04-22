import httpClient from '$lib/api/httpClient'; // Aggiornato con alias $lib
import { AuthResponse, type AuthResponseData } from '$lib/models/AuthResponse';

export class AuthService {
	// Percorso base allineato con @RequestMapping("/api/auth") in AuthController.java
	private static readonly basePath = '/api/auth';

	/**
	 * Invia le credenziali al backend e riceve il token JWT + dati utente.
	 */
	static async login(email: string, password: string): Promise<AuthResponse> {
		// Rimosso il try/catch: se c'è un errore (es. 401), viene catturato direttamente
		// dal blocco try/catch della pagina di login (+page.svelte) per aggiornare la UI.
		const response = await httpClient.post<AuthResponseData>(`${this.basePath}/login`, {
			email,
			password
		});

		const authData = new AuthResponse(response.data);

		// Salvataggio della sessione e del TOKEN nel localStorage
		this.setSession(authData);

		return authData;
	}

	private static setSession(authData: AuthResponse): void {
		// Salviamo il token JWT (fondamentale per le chiamate API successive e WebSocket)
		localStorage.setItem('jwt_token', authData.token);

		// Salviamo i dati per la UI
		localStorage.setItem('userId', authData.idUtente.toString());
		localStorage.setItem('userEmail', authData.email);
		localStorage.setItem('userRole', authData.ruolo);
		localStorage.setItem(
			'currentUser',
			JSON.stringify({
				idUtente: authData.idUtente,
				email: authData.email,
				ruolo: authData.ruolo
			})
		);
	}

	/**
	 * Recupera l'utente attualmente loggato.
	 */
	static getSession(): { idUtente: number; email: string; ruolo: string } | null {
		// Previene errori di Server-Side Rendering (SSR) in SvelteKit
		if (typeof window === 'undefined') return null;

		const userStr = localStorage.getItem('currentUser');
		return userStr ? JSON.parse(userStr) : null;
	}

	/**
	 * Recupera il token JWT salvato.
	 */
	static getToken(): string | null {
		if (typeof window === 'undefined') return null;

		return localStorage.getItem('jwt_token');
	}

	/**
	 * Comunica il logout al backend, pulisce la sessione ed esegue il redirect.
	 */
	static async logout(): Promise<void> {
		if (typeof window === 'undefined') return;

		try {
			// Comunichiamo al backend che l'utente sta uscendo (endpoint /logout)
			await httpClient.post(`${this.basePath}/logout`);
		} catch (error) {
			console.warn('Il server non ha risposto al logout, procedo con la pulizia locale.', error);
		} finally {
			// Puliamo tutto in locale indipendentemente dalla risposta del server
			localStorage.removeItem('jwt_token');
			localStorage.removeItem('userId');
			localStorage.removeItem('userEmail');
			localStorage.removeItem('userRole');
			localStorage.removeItem('currentUser');

			// Redirect alla pagina di login
			window.location.href = '/login';
		}
	}
}
