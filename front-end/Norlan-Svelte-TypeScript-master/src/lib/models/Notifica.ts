import { Priorita } from './Enums';

export interface NotificaData {
	idNotifica: number;
	idDestinatario: number;
	emailDestinatario: string;
	messaggio: string;
	letta: boolean;
	priorita: Priorita;
	dataInvio: string;
}

export class Notifica {
	idNotifica: number;
	idDestinatario: number;
	emailDestinatario: string;
	messaggio: string;
	letta: boolean;
	priorita: Priorita;
	dataInvio: string;

	constructor(data: NotificaData) {
		this.idNotifica = data.idNotifica;
		this.idDestinatario = data.idDestinatario;
		this.emailDestinatario = data.emailDestinatario;
		this.messaggio = data.messaggio;
		this.letta = data.letta;
		this.priorita = data.priorita;
		this.dataInvio = data.dataInvio;
	}
}
