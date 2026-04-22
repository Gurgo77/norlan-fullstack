import httpClient from '$lib/api/httpClient';
import { Azienda, type AziendaData } from '$lib/models/Azienda';
import type { Ruolo } from '$lib/models/Enums';

export interface AziendaCreateRequest {
    email: string;
    password?: string;
    ragioneSociale: string;
    partitaIva: string;
    sedeLegale?: string;
    pec?: string;
    telefono?: string;
    cellulare?: string;
    referenteAziendale?: string;
    hasDipendenti: boolean;
    ruolo: Ruolo;
}

export class AziendaService {
    // CORRETTO: Deve corrispondere a @RequestMapping + @GetMapping del controller
    private static basePath = '/api/anagrafica/aziende';

    static async getAll(): Promise<Azienda[]> {
        const response = await httpClient.get<AziendaData[]>(this.basePath);
        return response.data.map(data => new Azienda(data));
    }

    static async create(payload: AziendaCreateRequest): Promise<Azienda> {
        const response = await httpClient.post<AziendaData>(this.basePath, payload);
        return new Azienda(response.data);
    }

    static async delete(idUtente: number | string): Promise<void> {
        // Il controller usa /aziende/{id}
        await httpClient.delete(`${this.basePath}/${idUtente}`);
    }
}