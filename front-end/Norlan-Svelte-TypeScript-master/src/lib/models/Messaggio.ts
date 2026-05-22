/*
Modello di dominio per i messaggi scambiati all'interno della piattaforma.
Gestisce le informazioni su mittente, destinatario, contenuto testuale e stato di lettura.
*/
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