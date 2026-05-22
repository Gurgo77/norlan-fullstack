import { Priorita } from './Enums';
/*
Modello per la gestione degli alert e degli avvisi di sistema rivolti agli utenti.
Traccia i dettagli del messaggio, lo stato di lettura e il livello di priorità dell'avviso.
*/
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
