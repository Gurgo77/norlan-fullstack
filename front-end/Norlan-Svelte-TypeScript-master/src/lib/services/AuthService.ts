import httpClient from '$lib/api/httpClient';
import type { AuthRequest } from '$lib/models/AuthRequest';

export interface LoginRequest {
	email: string;
	password: string;
}

export interface LoginResponse {
	token: string;
	idUtente: number;
	email: string;
	ruolo: 'AZIENDA' | 'DOCENTE' | 'LAVORATORE' | 'ADMIN' | string;
	richiedeCambioPassword: boolean;
}

export interface UserSession {
	idUtente: number;
	email: string;
	ruolo: string;
	richiedeCambioPassword: boolean;
}

export class AuthService {
	private static readonly basePath = '/api/auth';

	static async login(credentials: AuthRequest): Promise<LoginResponse> {
		const response = await httpClient.post<LoginResponse>(`${this.basePath}/login`, credentials);
		const authData = response.data;
		this.setSession(authData);
		return authData;
	}

	private static setSession(authData: LoginResponse): void {
		if (typeof window === 'undefined') return;

		localStorage.setItem('jwt_token', authData.token);
		localStorage.setItem('userId', authData.idUtente.toString());
		localStorage.setItem('userEmail', authData.email);
		localStorage.setItem('userRole', authData.ruolo);

		const session: UserSession = {
			idUtente: authData.idUtente,
			email: authData.email,
			ruolo: authData.ruolo,
			richiedeCambioPassword: authData.richiedeCambioPassword ?? false
		};

		localStorage.setItem('currentUser', JSON.stringify(session));
	}

	static getSession(): UserSession | null {
		if (typeof window === 'undefined') return null;

		const userStr = localStorage.getItem('currentUser');
		return userStr ? (JSON.parse(userStr) as UserSession) : null;
	}

	static getToken(): string | null {
		if (typeof window === 'undefined') return null;
		return localStorage.getItem('jwt_token');
	}

	static async logout(): Promise<void> {
		if (typeof window === 'undefined') return;

		try {
			await httpClient.post(`${this.basePath}/logout`);
		} catch (error) {
			console.warn(
				'Errore di comunicazione con il server durante il logout. Esecuzione della pulizia locale dei dati.',
				error
			);
		} finally {
			localStorage.removeItem('jwt_token');
			localStorage.removeItem('userId');
			localStorage.removeItem('userEmail');
			localStorage.removeItem('userRole');
			localStorage.removeItem('currentUser');
			window.location.href = '/login';
		}
	}

	static async cambiaPassword(vecchiaPassword: string, nuovaPassword: string): Promise<string> {
		const payload = {
			vecchiaPassword,
			nuovaPassword
		};
		const response = await httpClient.put<string>(`${this.basePath}/cambia-password`, payload);
		return response.data;
	}

	static getDashboardRouteByRole(ruolo?: string): string {
		switch (ruolo) {
			case 'ADMIN':
				return '/dashboard/admin';
			case 'AZIENDA':
				return '/dashboard/azienda';
			case 'DOCENTE':
				return '/dashboard/docente';
			case 'LAVORATORE':
			case 'DIPENDENTE':
				return '/dashboard/dipendente';
			default:
				return '/login';
		}
	}
}
