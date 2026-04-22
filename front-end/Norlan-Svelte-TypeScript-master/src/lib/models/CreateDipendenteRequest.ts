// src/lib/models/CreateDipendenteRequest.ts

/**
 * Modello dedicato esclusivamente alla creazione di un nuovo dipendente.
 * Non contiene ID o dati dell'azienda perché gestiti dal backend tramite l'URL.
 */
export interface CreateDipendenteRequest {
	/** Nome del lavoratore. */
	nome: string;

	/** Cognome del lavoratore. */
	cognome: string;

	/** Email univoca per l'accesso al portale. */
	email: string;

	/** Codice Fiscale per la gestione della compliance e DPI. */
	codiceFiscale: string;
}
