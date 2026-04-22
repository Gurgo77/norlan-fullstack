import { StatoRinnovo } from './Enums';

export interface RichiestaRinnovoData {
	idRichiesta: number;
	idDocumento: number;
	tipologiaDocumento: string;
	ragioneSocialeAzienda: string;
	dataRichiesta: string;
	stato: StatoRinnovo;
}

export class RichiestaRinnovo {
	idRichiesta: number;
	idDocumento: number;
	tipologiaDocumento: string;
	ragioneSocialeAzienda: string;
	dataRichiesta: string;
	stato: StatoRinnovo;

	constructor(data: RichiestaRinnovoData) {
		this.idRichiesta = data.idRichiesta;
		this.idDocumento = data.idDocumento;
		this.tipologiaDocumento = data.tipologiaDocumento;
		this.ragioneSocialeAzienda = data.ragioneSocialeAzienda;
		this.dataRichiesta = data.dataRichiesta;
		this.stato = data.stato;
	}
}
