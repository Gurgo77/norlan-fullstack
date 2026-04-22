export interface MessaggioData {
	idMessaggio: number;
	idMittente: number;
	nomeMittente: string;
	idDestinatario: number;
	testo: string;
	timestampInvio: string;
	letto: boolean;
}

export class Messaggio {
	idMessaggio: number;
	idMittente: number;
	nomeMittente: string;
	idDestinatario: number;
	testo: string;
	timestampInvio: string;
	letto: boolean;

	constructor(data: MessaggioData) {
		this.idMessaggio = data.idMessaggio;
		this.idMittente = data.idMittente;
		this.nomeMittente = data.nomeMittente;
		this.idDestinatario = data.idDestinatario;
		this.testo = data.testo;
		this.timestampInvio = data.timestampInvio;
		this.letto = data.letto;
	}
}