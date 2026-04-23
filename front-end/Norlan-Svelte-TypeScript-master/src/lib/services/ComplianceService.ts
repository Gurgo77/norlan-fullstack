import httpClient from '$lib/api/httpClient';

export class ComplianceService {
	private static readonly basePath = '/api/compliance';

	/**
	 * Recupera lo stato di compliance (corsi fatti, DPI in regola, ecc.) per una specifica azienda.
	 * BE: GET /api/compliance/azienda/{idAzienda}
	 */
	static async getStatoComplianceAzienda(idAzienda: number | string) {
		const response = await httpClient.get(`${this.basePath}/azienda/${idAzienda}`);
		return response.data;
	}
}
