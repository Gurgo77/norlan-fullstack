import { Client } from '@stomp/stompjs';
import type { MessaggioRequest } from '$lib/models/MessaggioRequest';
import type { Messaggio } from '$lib/models/Messaggio';

let stompClient: Client;

export const chatService = {
	connect: (userId: number, onMessageReceived: (msg: Messaggio) => void) => {
		stompClient = new Client({
			brokerURL: 'ws://localhost:8080/ws',
			onConnect: () => {
				stompClient.subscribe(`/user/${userId}/queue/messages`, (message) => {
					onMessageReceived(JSON.parse(message.body));
				});
			}
		});
		stompClient.activate();
	},

	send: (payload: MessaggioRequest) => {
		if (stompClient && stompClient.connected) {
			stompClient.publish({
				destination: '/app/chat.send',
				body: JSON.stringify(payload)
			});
		}
	}
};
