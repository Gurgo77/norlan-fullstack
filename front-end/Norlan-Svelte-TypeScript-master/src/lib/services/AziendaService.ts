import httpClient from '$lib/api/httpClient';
import { Azienda, type AziendaData } from '$lib/models/Azienda';
import type { Ruolo } from '$lib/models/Enums';

// 1. Definiamo l'interfaccia per i dati necessari alla creazione
// Questo elimina l'errore "any" e ti dà l'autocompletamento
export interface AziendaCreateRequest {
    email: string;
    password?: string; // <--- AGGIUNGI QUESTA RIGA
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
    private static basePath = '/api/aziende';

    /**
     * Recupera tutte le aziende
     */
    static async getAll(): Promise<Azienda[]> {
        // Specifichiamo che il server risponde con un array di AziendaData
        const response = await httpClient.get<AziendaData[]>(this.basePath);
        return response.data.map((data) => new Azienda(data));
    }

    /**
     * Crea una nuova azienda
     * @param payload Sostituito 'any' con 'AziendaCreateRequest'
     */
    static async create(payload: AziendaCreateRequest): Promise<Azienda> {
        // Specifichiamo che la risposta sarà un oggetto AziendaData
        const response = await httpClient.post<AziendaData>(this.basePath, payload);
        return new Azienda(response.data);
    }

    /**
     * Elimina un'azienda tramite ID
     */
    static async delete(idUtente: number | string): Promise<void> {
        await httpClient.delete(`${this.basePath}/${idUtente}`);
    }
}
