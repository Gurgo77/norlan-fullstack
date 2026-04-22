import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import httpClient from '$lib/api/httpClient'; // Aggiornato con alias $lib
import { Messaggio, type MessaggioData } from '$lib/models/Messaggio';

export class ChatService {
	private client: Client | null = null;

	// CORRETTO: Allineato con @RequestMapping("/api/chat") del MessaggioController.java
	private static readonly basePath = '/api/chat';

	constructor(
		private onMessageReceived: (msg: Messaggio) => void,
		private onError: (err: string) => void
	) {}

	/**
	 * Avvia la connessione WebSocket
	 */
	connect(token: string, userId: number | string) {
		// Usa l'URL dell'httpClient (così se in futuro cambi da localhost a www.tuosito.it si aggiorna da solo)
		const baseUrl = httpClient.defaults.baseURL || 'http://localhost:8080';

		this.client = new Client({
			webSocketFactory: () => new SockJS(`${baseUrl}/ws`),
			connectHeaders: {
				Authorization: `Bearer ${token}`
			},
			onConnect: () => {
				// Iscrizione alla coda dei messaggi personali
				this.client?.subscribe(`/user/${userId}/queue/messages`, (message) => {
					const data: MessaggioData = JSON.parse(message.body);
					this.onMessageReceived(new Messaggio(data));
				});

				// Iscrizione alla coda degli errori (AccessDeniedException del controller)
				this.client?.subscribe(`/user/queue/errors`, (message) => {
					this.onError(message.body);
				});
			},
			onStompError: (frame) => {
				this.onError(frame.headers['message']);
			}
		});

		this.client.activate();
	}

	/**
	 * Chiude la connessione
	 */
	disconnect() {
		if (this.client) {
			this.client.deactivate();
		}
	}

	/**
	 * Invia un messaggio tramite WebSocket
	 */
	sendMessage(idMittente: number, idDestinatario: number, testo: string) {
		if (this.client && this.client.connected) {
			const payload = { idMittente, idDestinatario, testo };

			// CORRETTO: Allineato con @MessageMapping("/chat.send") in ChatController.java
			this.client.publish({
				destination: '/app/chat.send',
				body: JSON.stringify(payload)
			});
		} else {
			this.onError('Errore: Impossibile inviare il messaggio, la connessione chat non è attiva.');
		}
	}

	/**
	 * Recupera la cronologia dei messaggi tra due utenti tramite API REST
	 */
	static async getCronologia(id1: number, id2: number): Promise<Messaggio[]> {
		// Aggiunto <MessaggioData[]> per evitare errori TypeScript "any"
		const response = await httpClient.get<MessaggioData[]>(
			`${this.basePath}/cronologia/${id1}/${id2}`
		);
		return response.data.map((item) => new Messaggio(item));
	}
}
