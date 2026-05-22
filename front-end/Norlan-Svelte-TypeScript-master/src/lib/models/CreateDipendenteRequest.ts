/*
Payload DTO per la registrazione di un nuovo dipendente.
Specifica i dati anagrafici e di contatto richiesti per l'onboarding.
*/
export interface CreateDipendenteRequest {
	nome: string;
	cognome: string;
	email: string;
	codiceFiscale: string;
}
