import type { TipoDPI } from './Enums';
/*
Payload per le richieste di assegnazione di un nuovo DPI a un dipendente.
Definisce i dati obbligatori necessari per l'inserimento del record a sistema.
*/
export interface AssegnazioneDPIRequest {
	idDipendente: number;
	tipo: TipoDPI;
	dataScadenzaRevisione: string;
	nomeDpi?: string;
}
