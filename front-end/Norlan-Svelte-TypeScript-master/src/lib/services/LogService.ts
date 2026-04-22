// src/lib/services/LogService.ts
import httpClient from '../api/httpClient';
import { LogSincronizzazione, type LogSincronizzazioneData } from '../models/LogSincronizzazione';

export class LogService {
  private static readonly endpoint = '/api/sistema/logs';

  /**
   * Recupera la cronologia dei log di sincronizzazione.
   * Endpoint: GET /api/sistema/logs
   */
  static async getLogs(): Promise<LogSincronizzazione[]> {
    const response = await httpClient.get<LogSincronizzazioneData[]>(this.endpoint);
    return response.data.map((item: LogSincronizzazioneData) => new LogSincronizzazione(item));
  }
}