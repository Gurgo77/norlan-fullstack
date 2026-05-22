/*
Payload DTO per la creazione e l'invio di un nuovo messaggio.
Specifica i parametri essenziali necessari per il routing e la consegna al destinatario.
*/
export interface MessaggioRequest {
	idMittente: number;
	idDestinatario: number;
	testo: string;
}