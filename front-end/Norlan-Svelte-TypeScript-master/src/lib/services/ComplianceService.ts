import httpClient from '$lib/api/httpClient';

export class ComplianceService {
	private static readonly basePath = '/api/compliance';

	static async getStatoComplianceAzienda(idAzienda: number | string) {
		const response = await httpClient.get(`${this.basePath}/azienda/${idAzienda}`);
		return response.data;
	}
}
