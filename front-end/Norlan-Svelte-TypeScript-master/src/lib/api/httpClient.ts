/**
 * Client HTTP centralizzato.
 * Configura l'URL di base e gestisce automaticamente l'iniezione del token JWT
 * e la disconnessione in caso di sessione scaduta.
 */

import axios, { type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';

const httpClient = axios.create({
	baseURL: 'http://localhost:8080',
	headers: {
		'Content-Type': 'application/json'
	}
});

// Inietta il token JWT in ogni richiesta in uscita, se presente
httpClient.interceptors.request.use(
	(config: InternalAxiosRequestConfig) => {
		if (typeof window !== 'undefined') {
			const token = localStorage.getItem('jwt_token');

			if (token && config.headers) {
				config.headers.Authorization = `Bearer ${token}`;
			}
		}
		return config;
	},
	(error) => Promise.reject(error)
);

// Intercetta gli errori 401/403 per forzare il logout se il token è scaduto
httpClient.interceptors.response.use(
	(response: AxiosResponse) => response,
	(error) => {
		// Escludiamo la rotta '/login' per evitare loop infiniti di reindirizzamento
		if (
			error.response &&
			(error.response.status === 401 || error.response.status === 403) &&
			error.config &&
			!error.config.url?.includes('login')
		) {
			console.warn('Sessione scaduta: reindirizzamento al login.');

			if (typeof window !== 'undefined') {
				localStorage.clear();
				window.location.href = '/login';
			}
		}

		return Promise.reject(error);
	}
);

export default httpClient;