import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import httpClient from '$lib/api/httpClient';
import { Messaggio, type MessaggioData } from '$lib/models/Messaggio';

export interface ChatMessagePayload {
	idMittente: number;
	idDestinatario: number;
	testo: string;
}

export class ChatService {
	private client: Client | null = null;

	private static readonly basePath = '/api/chat';

	constructor(
		private onMessageReceived: (msg: Messaggio) => void,
		private onError: (err: string) => void
	) {}

	connect(token: string, userId: number | string): void {
		if (typeof window !== 'undefined' && !(window as any).global) {
			(window as any).global = window;
		}

		const baseUrl = (httpClient.defaults.baseURL || 'http://localhost:8080').replace(/\/$/, '');

		this.client = new Client({
			debug: (str) => console.log('STOMP: ' + str),

			webSocketFactory: () => new SockJS(`${baseUrl}/ws`),
			connectHeaders: {
				Authorization: `Bearer ${token}`
			},
			onConnect: () => {
				console.log('✅ Connessione Chat STOMP Stabilita con successo!');

				this.client?.subscribe(`/user/${userId}/queue/messages`, (message) => {
					const data: MessaggioData = JSON.parse(message.body);
					this.onMessageReceived(new Messaggio(data));
				});

				this.client?.subscribe(`/user/queue/errors`, (message) => {
					this.onError(message.body);
				});
			},
			onStompError: (frame) => {
				console.error('❌ STOMP Error:', frame.headers['message']);
				this.onError(frame.headers['message'] || 'Errore di connessione STOMP al server');
			},
			onWebSocketError: (event) => {
				console.error('❌ Errore critico WebSocket:', event);
			}
		});

		this.client.activate();
	}

	disconnect(): void {
		if (this.client && this.client.active) {
			this.client.deactivate();
		}
	}

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

	static async getCronologia(id1: number, id2: number): Promise<Messaggio[]> {
		const response = await httpClient.get<MessaggioData[]>(
			`${this.basePath}/cronologia/${id1}/${id2}`
		);
		return response.data.map((item) => new Messaggio(item));
	}
}
