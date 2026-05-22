import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import httpClient from '$lib/api/httpClient';
import { Messaggio, type MessaggioData } from '$lib/models/Messaggio';
/*
Servizio per la gestione della comunicazione in tempo reale tramite WebSocket (protocollo STOMP).
Gestisce la connessione, l'abbonamento ai canali privati e il recupero dello storico messaggi.
*/
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

	// Inizializza il client STOMP con fallback WebSocket, configura i listener e avvia la connessione
	connect(token: string, userId: number | string): void {
		if (typeof window !== 'undefined' && !('global' in window)) {
			(window as Window & { global?: Window }).global = window;
		}

		const baseUrl = (httpClient.defaults.baseURL || 'http://localhost:8080').replace(/\/$/, '');

		this.client = new Client({
			debug: (str) => console.log('STOMP: ' + str),

			webSocketFactory: () => new SockJS(`${baseUrl}/ws`),
			connectHeaders: {
				Authorization: `Bearer ${token}`
			},
			onConnect: () => {
				console.log('Connessione Chat STOMP stabilita con successo.');

				this.client?.subscribe(`/user/${userId}/queue/messages`, (message) => {
					const data: MessaggioData = JSON.parse(message.body);
					this.onMessageReceived(new Messaggio(data));
				});

				this.client?.subscribe(`/user/queue/errors`, (message) => {
					this.onError(message.body);
				});
			},
			onStompError: (frame) => {
				console.error('Errore STOMP riscontrato:', frame.headers['message']);
				this.onError(frame.headers['message'] || 'Errore di connessione STOMP al server');
			},
			onWebSocketError: (event) => {
				console.error('Errore critico WebSocket:', event);
			}
		});

		this.client.activate();
	}

	// Disattiva la connessione WebSocket e libera le risorse del client STOMP
	disconnect(): void {
		if (this.client && this.client.active) {
			this.client.deactivate();
		}
	}

	// Pubblica un nuovo messaggio sul canale di destinazione dopo aver verificato lo stato attivo della connessione
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

	// Recupera l'intero storico dei messaggi tra due utenti tramite una richiesta HTTP REST
	static async getCronologia(id1: number, id2: number): Promise<Messaggio[]> {
		const response = await httpClient.get<MessaggioData[]>(
			`${this.basePath}/cronologia/${id1}/${id2}`
		);
		return response.data.map((item) => new Messaggio(item));
	}
}
