import type { TipoDPI } from './Enums';

export interface AssegnazioneDPIRequest {
	idDipendente: number;
	tipo: TipoDPI;
	dataScadenzaRevisione: string;
	nomeDpi?: string;
}
