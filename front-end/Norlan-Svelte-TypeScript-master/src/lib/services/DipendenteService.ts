import httpClient from '$lib/api/httpClient';
import { Dipendente, type DipendenteData } from '$lib/models/Dipendente';
import { Ruolo } from '$lib/models/Enums';

/**
 * Interfaccia per la creazione di un nuovo dipendente.
 * Utilizzata per tipizzare il payload ed evitare errori 'any'.
 */
export interface DipendenteCreateRequest {
    email: string;
    password?: string;
    nome: string;
    cognome: string;
    codiceFiscale: string;
    mansione?: string;
    idAzienda: number | string;
    ruolo: Ruolo;
}

export class DipendenteService {
    // Percorso base API (regolalo in base al tuo controller Spring Boot)
    private static basePath = '/api/dipendenti';

    /**
     * Recupera la lista completa di tutti i dipendenti registrati nel sistema.
     */
    static async getAll(): Promise<Dipendente[]> {
        const response = await httpClient.get<DipendenteData[]>(this.basePath);
        // Trasformiamo i dati JSON nelle istanze della classe Dipendente per usare i metodi del modello
        return response.data.map((data) => new Dipendente(data));
    }

    /**
     * Recupera i dipendenti appartenenti a una specifica azienda.
     * @param idAzienda L'ID dell'azienda di cui cercare i lavoratori
     */
    static async getByAzienda(idAzienda: number | string): Promise<Dipendente[]> {
        const response = await httpClient.get<DipendenteData[]>(
            `${this.basePath}/azienda/${idAzienda}`
        );
        return response.data.map((data) => new Dipendente(data));
    }

    /**
     * Recupera i dettagli di un singolo dipendente tramite il suo ID.
     */
    static async getById(id: number | string): Promise<Dipendente> {
        const response = await httpClient.get<DipendenteData>(`${this.basePath}/${id}`);
        return new Dipendente(response.data);
    }

    /**
     * Crea un nuovo dipendente nel sistema.
     * @param payload I dati del dipendente (nome, cognome, azienda di appartenenza, ecc.)
     */
    static async create(payload: DipendenteCreateRequest): Promise<Dipendente> {
        const response = await httpClient.post<DipendenteData>(this.basePath, payload);
        return new Dipendente(response.data);
    }

    /**
     * Elimina un dipendente dal sistema.
     * @param id L'ID dell'utente/dipendente da rimuovere
     */
    static async delete(id: number | string): Promise<void> {
        await httpClient.delete(`${this.basePath}/${id}`);
    }
}
