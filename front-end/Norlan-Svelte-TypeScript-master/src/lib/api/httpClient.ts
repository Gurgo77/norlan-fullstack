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
		// Controllo SSR: ci assicuriamo di essere nel browser prima di accedere al localStorage
		if (typeof window !== 'undefined') {
			const token = localStorage.getItem('jwt_token');

			if (token && config.headers) {
				// Formato standard Bearer richiesto dal backend
				config.headers.Authorization = `Bearer ${token}`;
			}
		}

		return config;
	},
	(error) => {
		return Promise.reject(error);
	}
);

/**
 * INTERCETTORE DI RISPOSTA (PROTEZIONE SUPREMA):
 * Gestisce centralmente gli errori HTTP comuni, intercettando la scadenza del token (401).
 */
httpClient.interceptors.response.use(
	(response: AxiosResponse) => response,
	(error) => {
		// Se il server risponde 401 (Unauthorized), il token è scaduto, contraffatto o assente
		if (error.response && error.response.status === 401) {
			console.warn('Sessione scaduta o non valida. Disconnessione di sicurezza in corso...');

			// Controllo SSR per manipolazione DOM/Storage
			if (typeof window !== 'undefined') {
				// Distruzione totale della sessione locale per evitare infinite-loop
				localStorage.clear();

				// Reindirizzamento brutale: distrugge lo stato di SvelteKit e ricarica l'app pulita
				window.location.href = '/login';
			}
		}

		// Rimbalza l'errore ai file Service in modo che possano gestire eventuali alert visivi (catch)
		return Promise.reject(error);
	}
);

export default httpClient;