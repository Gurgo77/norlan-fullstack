/*
Payload DTO per la sottomissione di una nuova richiesta di rinnovo.
Definisce l'identificativo del documento da rinnovare e permette l'inserimento di eventuali note opzionali.
*/
export interface RichiestaRinnovoRequest {
	idDocumento: number;
	note?: string;
}
