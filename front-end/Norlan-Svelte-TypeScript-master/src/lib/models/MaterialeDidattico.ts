/*
Entità per la gestione delle risorse e dei documenti associati ai corsi di formazione.
Mantiene i riferimenti ai file caricati, collegandoli al corso specifico e alla data di upload.
*/
export interface MaterialeDidatticoData {
	idMateriale: number;
	idCorso: number;
	titoloCorso: string;
	titoloDocumento: string;
	percorsoFile: string;
	dataCaricamento: string;
}

export class MaterialeDidattico {
	idMateriale: number;
	idCorso: number;
	titoloCorso: string;
	titoloDocumento: string;
	percorsoFile: string;
	dataCaricamento: string;

	constructor(data: MaterialeDidatticoData) {
		this.idMateriale = data.idMateriale;
		this.idCorso = data.idCorso;
		this.titoloCorso = data.titoloCorso;
		this.titoloDocumento = data.titoloDocumento;
		this.percorsoFile = data.percorsoFile;
		this.dataCaricamento = data.dataCaricamento;
	}
}
