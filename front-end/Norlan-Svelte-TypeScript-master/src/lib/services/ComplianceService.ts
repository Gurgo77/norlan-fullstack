import httpClient from '$lib/api/httpClient';
/*
Servizio per la verifica e il monitoraggio dello stato di compliance aziendale.
Interroga l'endpoint dedicato per valutare il rispetto delle normative vigenti.
*/
export class ComplianceService {
	private static readonly basePath = '/api/compliance';

	// Recupera il report aggiornato sullo stato di conformità normativa associato a una specifica azienda
	static async getStatoComplianceAzienda(idAzienda: number | string) {
		const response = await httpClient.get(`${this.basePath}/azienda/${idAzienda}`);
		return response.data;
	}
}
