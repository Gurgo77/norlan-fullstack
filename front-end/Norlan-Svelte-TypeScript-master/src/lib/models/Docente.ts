export interface DocenteData {
	idUtente: number;
	nome: string;
	cognome: string;
	titolo: string;
	email: string;
	specializzazioneTecnica: string;
	bio: string;
}

export class Docente {
	idUtente: number;
	nome: string;
	cognome: string;
	titolo: string;
	email: string;
	specializzazioneTecnica: string;
	bio: string;

	constructor(data: DocenteData) {
		this.idUtente = data.idUtente;
		this.nome = data.nome;
		this.cognome = data.cognome;
		this.titolo = data.titolo;
		this.email = data.email;
		this.specializzazioneTecnica = data.specializzazioneTecnica;
		this.bio = data.bio;
	}
}