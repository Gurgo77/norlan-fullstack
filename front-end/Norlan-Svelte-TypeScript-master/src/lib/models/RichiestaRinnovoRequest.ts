// src/lib/models/RichiestaRinnovoRequest.ts

export interface RichiestaRinnovoRequest {
	/**
	 * L'ID del documento (DVR, HACCP, ecc.) per cui si richiede il rinnovo.
	 */
	idDocumento: number;

	/**
	 * Note opzionali aggiunte dall'azienda per lo staff NorLan.
	 */
	note?: string;
}
