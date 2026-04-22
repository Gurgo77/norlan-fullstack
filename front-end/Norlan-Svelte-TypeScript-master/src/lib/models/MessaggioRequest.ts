// src/lib/models/MessaggioRequest.ts

export interface MessaggioRequest {
	/**
	 * L'ID dell'utente che invia il messaggio.
	 * Corrisponde a payload.getIdMittente() richiesto dal backend.
	 */
	idMittente: number;

	/**
	 * L'ID dell'utente che riceverà il messaggio.
	 * Necessario per l'instradamento nel sistema multithreading del backend.
	 */
	idDestinatario: number;

	/**
	 * Il contenuto testuale del messaggio.
	 */
	testo: string;
}