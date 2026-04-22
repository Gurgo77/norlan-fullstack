import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import httpClient from '../api/httpClient';
import { Messaggio, type MessaggioData } from '../models/Messaggio';

export class ChatService {
	private client: Client | null = null;
	private static readonly endpoint = '/api/comunicazioni';

	constructor(
		private onMessageReceived: (msg: Messaggio) => void,
		private onError: (err: string) => void
	) {}

	connect(token: string, userId: number) {
		this.client = new Client({
			webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
			connectHeaders: {
				Authorization: `Bearer ${token}`
			},
			onConnect: () => {
				this.client?.subscribe(`/user/${userId}/queue/messages`, (message) => {
					const data: MessaggioData = JSON.parse(message.body);
					this.onMessageReceived(new Messaggio(data));
				});

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

	disconnect() {
		if (this.client) {
			this.client.deactivate();
		}
	}

	sendMessage(idMittente: number, idDestinatario: number, testo: string) {
		if (this.client && this.client.connected) {
			const payload = { idMittente, idDestinatario, testo };

			// IMPORTANTE: cambiato da comunicazioni.send a chat.send
			this.client.publish({
				destination: '/app/chat.send',
				body: JSON.stringify(payload)
			});
		} else {
			// Aggiunta la gestione dell'errore se si è offline
			this.onError("Errore: Impossibile inviare il messaggio, la connessione chat non è attiva.");
		}
	}

	static async getCronologia(id1: number, id2: number): Promise<Messaggio[]> {
		const response = await httpClient.get(`${this.endpoint}/cronologia/${id1}/${id2}`);
		return response.data.map((item: MessaggioData) => new Messaggio(item));
	}
}