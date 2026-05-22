import { ModuloServizio, TipoDocumento, StatoDocumento } from './Enums';
/*
Modello per la gestione documentale aziendale.
Mappa i metadati dei file caricati, il loro stato di approvazione e le relative scadenze di validità.
*/
export interface DocumentoData {
	idDocumento: number;
	idAzienda: number;
	ragioneSocialeAzienda: string;
	modulo: ModuloServizio;
	tipologia: TipoDocumento;
	stato: StatoDocumento;
	filePath: string;
	dataCaricamento: string;
	dataScadenza: string;
	scaduto: boolean;
}

export class Documento {
	idDocumento: number;
	idAzienda: number;
	ragioneSocialeAzienda: string;
	modulo: ModuloServizio;
	tipologia: TipoDocumento;
	stato: StatoDocumento;
	filePath: string;
	dataCaricamento: string;
	dataScadenza: string;
	scaduto: boolean;

	constructor(data: DocumentoData) {
		this.idDocumento = data.idDocumento;
		this.idAzienda = data.idAzienda;
		this.ragioneSocialeAzienda = data.ragioneSocialeAzienda;
		this.modulo = data.modulo;
		this.tipologia = data.tipologia;
		this.stato = data.stato;
		this.filePath = data.filePath;
		this.dataCaricamento = data.dataCaricamento;
		this.dataScadenza = data.dataScadenza;
		this.scaduto = data.scaduto;
	}
}
