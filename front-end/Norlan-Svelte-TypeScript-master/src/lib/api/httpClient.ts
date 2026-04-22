import axios, { type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';

// Creiamo l'istanza configurata per il portale Norlan
const httpClient = axios.create({
	// La porta 8080 è quella definita nel tuo backend Spring Boot
	baseURL: 'http://localhost:8080',
	headers: {
		'Content-Type': 'application/json'
	}
});

/**
 * INTERCETTORE DI RICHIESTA (JWT):
 * Prima di ogni chiamata, controlla se esiste un token nel localStorage.
 * Se presente, lo inserisce nell'header Authorization.
 */
httpClient.interceptors.request.use(
	(config: InternalAxiosRequestConfig) => {
		const token = localStorage.getItem('jwt_token');

		if (token && config.headers) {
			// Formato standard Bearer richiesto dal backend
			config.headers.Authorization = `Bearer ${token}`;
		}

		return config;
	},
	(error) => {
		return Promise.reject(error);
	}
);

/**
 * INTERCETTORE DI RISPOSTA:
 * Gestisce centralmente gli errori HTTP comuni, come il 401 (Unauthorized).
 */
httpClient.interceptors.response.use(
	(response: AxiosResponse) => response,
	(error) => {
		// Se il server risponde 401, il token è scaduto o non valido
		if (error.response && error.response.status === 401) {
			console.warn('Sessione scaduta o non valida. Pulizia dati in corso...');

			// Puliamo il localStorage per evitare loop di richieste con token errati
			localStorage.clear();

			// Reindirizzamento forzato alla pagina di login se siamo nel browser
			if (typeof window !== 'undefined') {
				window.location.href = '/login';
			}
		}

		// Ritorna l'errore per permettere ai Service di gestirlo (es. mostrare un toast)
		return Promise.reject(error);
	}
);

export default httpClient;
