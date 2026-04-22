// src/lib/services/AuthService.ts
import httpClient from '../api/httpClient';
import { AuthResponse, type AuthResponseData } from '../models/AuthResponse';

export class AuthService {
	private static readonly endpoint = '/api/auth';

	/**
	 * Invia le credenziali al backend e riceve il token JWT + dati utente.
	 */
	static async login(email: string, password: string): Promise<AuthResponse> {
		try {
			// Chiamata REALE all'endpoint Spring Security
			const response = await httpClient.post<AuthResponseData>(`${this.endpoint}/login`, {
				email,
				password
			});

			const authData = new AuthResponse(response.data);

			// Salvataggio della sessione e del TOKEN nel localStorage
			this.setSession(authData);

			return authData;
		} catch (error) {
			console.error('Errore di autenticazione:', error);
			throw new Error('Credenziali non valide. Riprova.');
		}
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
		const userStr = localStorage.getItem('currentUser');
		return userStr ? JSON.parse(userStr) : null;
	}

	/**
	 * Recupera il token JWT salvato.
	 */
	static getToken(): string | null {
		return localStorage.getItem('jwt_token');
	}

	/**
	 * Pulisce la sessione ed esegue il redirect al login.
	 */
	static logout(): void {
		localStorage.removeItem('jwt_token');
		localStorage.removeItem('userId');
		localStorage.removeItem('userEmail');
		localStorage.removeItem('userRole');
		localStorage.removeItem('currentUser');

		window.location.href = '/login';
	}
}
