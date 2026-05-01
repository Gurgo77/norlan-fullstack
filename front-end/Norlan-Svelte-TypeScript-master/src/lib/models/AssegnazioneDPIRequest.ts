// src/lib/models/AssegnazioneDPIRequest.ts
import type { TipoDPI } from './Enums';

export interface AssegnazioneDPIRequest {
	idDipendente: number;
	tipo: TipoDPI;
	dataScadenzaRevisione: string; // ISO date
	nomeDpi?: string;
}
