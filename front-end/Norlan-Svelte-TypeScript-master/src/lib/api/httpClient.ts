import axios, { type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';

const httpClient = axios.create({
	baseURL: 'http://localhost:8080',
	headers: {
		'Content-Type': 'application/json'
	}
});

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
	(error) => {
		return Promise.reject(error);
	}
);

httpClient.interceptors.response.use(
	(response: AxiosResponse) => response,
	(error) => {
		if (
			error.response &&
			(error.response.status === 401 || error.response.status === 403) &&
			error.config &&
			!error.config.url?.includes('login')
		) {
			console.warn('Sessione scaduta o accesso negato. Disconnessione di sicurezza in corso...');

			if (typeof window !== 'undefined') {
				localStorage.clear();
				window.location.href = '/login';
			}
		}

		return Promise.reject(error);
	}
);

export default httpClient;