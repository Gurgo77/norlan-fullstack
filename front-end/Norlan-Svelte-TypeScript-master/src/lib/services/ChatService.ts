import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import httpClient from '$lib/api/httpClient';
import { Messaggio, type MessaggioData } from '$lib/models/Messaggio';

// Interfaccia rigorosa per il payload in uscita (basata su MessaggioDTO del backend)
export interface ChatMessagePayload {
	idMittente: number;
	idDestinatario: number;
	testo: string;
}

export class ChatService {
	private client: Client | null = null;

	// Allineato con @RequestMapping("/api/chat") del MessaggioController (per la parte REST)
	private static readonly basePath = '/api/chat';

	/**
	 * @param onMessageReceived Callback chiamata quando arriva un nuovo messaggio
	 * @param onError Callback chiamata in caso di errori WebSocket o di Accesso Negato
	 */
	constructor(
		private onMessageReceived: (msg: Messaggio) => void,
		private onError: (err: string) => void
	) {}

	/**
	 * Avvia la connessione WebSocket
	 */
	connect(token: string, userId: number | string): void {
		const baseUrl = httpClient.defaults.baseURL || 'http://localhost:8080';

		this.client = new Client({
			webSocketFactory: () => new SockJS(`${baseUrl}/ws`),
			connectHeaders: {
				Authorization: `Bearer ${token}`
			},
			onConnect: () => {
				// Sottoscrizione ai messaggi in entrata
				// BE: messagingTemplate.convertAndSendToUser(idDestinatario, "/queue/messages", dto)
				this.client?.subscribe(`/user/${userId}/queue/messages`, (message) => {
					const data: MessaggioData = JSON.parse(message.body);
					this.onMessageReceived(new Messaggio(data));
				});

				// Sottoscrizione alla coda degli errori (AccessDeniedException)
				// BE: messagingTemplate.convertAndSendToUser(principal.getName(), "/queue/errors", e.getMessage())
				this.client?.subscribe(`/user/queue/errors`, (message) => {
					this.onError(message.body);
				});
			},
			onStompError: (frame) => {
				this.onError(frame.headers['message'] || 'Errore di connessione STOMP');
			}
		});

		this.client.activate();
	}

	/**
	 * Chiude la connessione (da chiamare nel blocco onDestroy() dei componenti Svelte)
	 */
	disconnect(): void {
		if (this.client && this.client.active) {
			this.client.deactivate();
		}
	}

	/**
	 * Invia un messaggio in tempo reale tramite WebSocket
	 * BE: @MessageMapping("/chat.send")
	 */
	sendMessage(payload: ChatMessagePayload): void {
		if (this.client && this.client.connected) {
			this.client.publish({
				destination: '/app/chat.send',
				body: JSON.stringify(payload)
			});
		} else {
			this.onError('Errore: Impossibile inviare il messaggio, la connessione chat non è attiva.');
		}
	}

	/**
	 * Recupera la cronologia dei messaggi storici tra due utenti tramite API REST standard
	 */
	static async getCronologia(id1: number, id2: number): Promise<Messaggio[]> {
		const response = await httpClient.get<MessaggioData[]>(
			`${this.basePath}/cronologia/${id1}/${id2}`
		);
		return response.data.map((item) => new Messaggio(item));
	}
}
