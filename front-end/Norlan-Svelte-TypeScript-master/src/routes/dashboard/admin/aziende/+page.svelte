<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { fade, scale, slide } from 'svelte/transition';
	import { goto } from '$app/navigation';
	import { resolveRoute } from '$app/paths';
	import {
		Building2, Plus, Trash2, ChevronLeft,
		Loader2, Search, Phone, User, Users, UserCheck, MapPin,
		FileText, AlertTriangle, Upload, UserPlus,
		Mail, Edit3
	} from 'lucide-svelte';

	import { Azienda, type AziendaData } from '$lib/models/Azienda';
	import { Documento } from '$lib/models/Documento';
	import { AnagraficaService, type AuthRequestDTO } from '$lib/services/AnagraficaService';
	import { LavoratoreService, type DipendenteDTO, type DipendenteRequest } from '$lib/services/LavoratoreService';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import { ModuloServizio, TipoDocumento } from '$lib/models/Enums';
	import DettagliCard from '$lib/Components/Features/Anagrafica/DettagliCard.svelte';
	import DocCard, { type DocInfo } from '$lib/Components/Features/Documentale/DocumentoCard.svelte';
	import DipendenteCard from '$lib/Components/Features/Anagrafica/DipendenteCard.svelte';
	import AziendaCard from '$lib/Components/Features/Anagrafica/AziendaCard.svelte';
	import ModalCard from '$lib/Components/UI/ModalCard.svelte';
	import { caricaDettagliEntita } from '$lib/utils/dettaglioUtils';
	import { creaUtenteUniversale, aggiornaUtenteUniversale, eliminaUtenteUniversale } from '$lib/utils/anagraficaUtils';
	import { uploadDocumentoUniversale, scaricaDocumentoUniversale, eliminaDocumentoUniversale } from '$lib/utils/documentoUtils';
	import { getInfoScadenza, formattaDataScadenza } from '$lib/utils/scadenzeUtils';

	let aziende = $state<Azienda[]>([]);
	let dipendentiCorrenti = $state<DipendenteDTO[]>([]);
	let documentiCorrenti = $state<Documento[]>([]);
	let isLoading = $state(true);
	let searchQuery = $state('');

	let selectedAzienda = $state<Azienda | null>(null);
	let dynamicHasDipendenti = $state(false);
	let showModal = $state(false);
	let isSaving = $state(false);
	let isEditing = $state(false);

	let showUploadModal = $state(false);
	let isUploading = $state(false);
	let uploadFile = $state<File | null>(null);
	let formDocumento = $state({ modulo: ModuloServizio.SICUREZZA, tipologia: TipoDocumento.DVR, dataRilascio: '', dataScadenza: '' });
	let idDocumentoDaAggiornare = $state<number | null>(null);

	let showDipendenteModal = $state(false);
	let isSavingDipendente = $state(false);
	let formDipendente = $state({ nome: '', cognome: '', codiceFiscale: '', email: '', password: '' });

	let showDeleteDocModal = $state(false);
	let docDaEliminare = $state<Documento | null>(null);
	let showDeleteDipModal = $state(false);
	let dipDaEliminare = $state<DipendenteDTO | null>(null);

	let formAzienda = $state({
		idUtente: null as number | null,
		email: '',
		password: '',
		ragioneSociale: '',
		partitaIva: '',
		sedeLegale: '',
		pec: '',
		telefono: '',
		cellulare: '',
		referenteAziendale: '',
		hasDipendenti: false
	});

	let showDeleteModal = $state(false);
	let aziendaDaEliminare = $state<Azienda | null>(null);
	let confermaTesto = $state('');

	const isFormValid = $derived(
			formAzienda.ragioneSociale.trim() !== '' &&
			formAzienda.partitaIva.length === 11 &&
			formAzienda.email.trim() !== '' &&
			(isEditing ? true : formAzienda.password.trim() !== '')
	);

	const isDipendenteValid = $derived(
			formDipendente.nome.trim() !== '' &&
			formDipendente.cognome.trim() !== '' &&
			formDipendente.codiceFiscale.length === 16 &&
			formDipendente.email.trim() !== '' &&
			formDipendente.password.trim() !== ''
	);

	const filteredAziende = $derived(aziende.filter(a => a.ragioneSociale.toLowerCase().includes(searchQuery.toLowerCase())));
	const isConfermaValida = $derived(confermaTesto.trim().toUpperCase() === 'ELIMINA');

	const documentiAziendaliFiltrati = $derived(documentiCorrenti.filter(doc => doc.tipologia !== 'ATTESTATO_CORSO'));

	onMount(async () => {
		try {
			const res = await AnagraficaService.getAllAziende();
			const data = res as AziendaData[];
			aziende = data.map(item => new Azienda(item));

			await Promise.all(aziende.map(async (a, i) => {
				aziende[i].hasDipendenti = await AnagraficaService.hasDipendenti(a.idUtente);
			}));

			const idDaUrl = $page.url.searchParams.get('id');
			if (idDaUrl) {
				const aziendaTrovata = aziende.find(a => String(a.idUtente) === idDaUrl);
				if (aziendaTrovata) {
					await apriDettaglio(aziendaTrovata);
				}
			}

		} catch {
			console.error("Errore durante l'inizializzazione del componente.");
		} finally {
			isLoading = false;
		}
	});

	async function apriDettaglio(aziendaPreview: Azienda) {
		selectedAzienda = aziendaPreview;

		const res = await caricaDettagliEntita(aziendaPreview.idUtente, 'AZIENDA') as any;

		if (res.error) {
			alert("Si è verificato un errore nel caricamento dei dati dell'azienda.");
		}

		selectedAzienda = res.info ?? null;
		documentiCorrenti = res.documenti;
		dipendentiCorrenti = res.personale;
		dynamicHasDipendenti = res.extra.hasDip;

		const idx = aziende.findIndex(a => a.idUtente === aziendaPreview.idUtente);
		if (idx !== -1) aziende[idx].hasDipendenti = res.extra.hasDip;
	}

	async function vaiADettaglioDipendente(idUtente: string | number) {
		return await goto(`${resolveRoute('/dashboard/admin/dipendenti')}?id=${idUtente}`);
	}

	function apreGmail(email: string) {
		if (!email) return;
		window.open(`https://mail.google.com/mail/?view=cm&fs=1&to=${email}`, '_blank');
	}

	async function vaiInChat(idUtente: string | number | undefined) {
		if (!idUtente) return;
		return await goto(`${resolveRoute('/dashboard/admin/comunicazioni')}?chatId=${idUtente}`);
	}

	function preparaEliminaDoc(doc: Documento) { docDaEliminare = doc; showDeleteDocModal = true; }

	async function confermaEliminaDoc() {
		if (!docDaEliminare) return;
		const res = await eliminaDocumentoUniversale(docDaEliminare.idDocumento);
		if (res.error) {
			alert(res.msg);
		} else {
			documentiCorrenti = documentiCorrenti.filter(d => d.idDocumento !== docDaEliminare?.idDocumento);
			showDeleteDocModal = false;
			docDaEliminare = null;
		}
	}

	function preparaEliminaDip(dip: DipendenteDTO) { dipDaEliminare = dip; showDeleteDipModal = true; }

	async function confermaEliminaDip() {
		if (!dipDaEliminare || !selectedAzienda) return;

		const res = await eliminaUtenteUniversale('DIPENDENTE', dipDaEliminare.idUtente);

		if (res.error) {
			alert(res.msg);
		} else {
			dipendentiCorrenti = dipendentiCorrenti.filter(d => d.idUtente !== dipDaEliminare?.idUtente);
			if (dipendentiCorrenti.length === 0) {
				dynamicHasDipendenti = false;
				const idx = aziende.findIndex(a => a.idUtente === selectedAzienda?.idUtente);
				if (idx !== -1) aziende[idx].hasDipendenti = false;
			}
			showDeleteDipModal = false;
			dipDaEliminare = null;
		}
	}

	async function salvaNuovoDipendente() {
		if (!isDipendenteValid || !selectedAzienda) return;

		const idAziendaCorrente = selectedAzienda.idUtente;

		isSavingDipendente = true;

		const payload = {
			idAzienda: idAziendaCorrente,
			nome: formDipendente.nome,
			cognome: formDipendente.cognome,
			codiceFiscale: formDipendente.codiceFiscale.toUpperCase(),
			email: formDipendente.email.trim(),
			passwordHash: formDipendente.password,
			richiedeCambioPassword: true
		};

		const res = await creaUtenteUniversale<DipendenteDTO>('DIPENDENTE', payload);

		if (res.error || !res.data) {
			alert(res.msg);
		} else {
			dipendentiCorrenti = [...dipendentiCorrenti, res.data];

			if (!dynamicHasDipendenti) {
				dynamicHasDipendenti = true;
				const idx = aziende.findIndex(a => a.idUtente === idAziendaCorrente);
				if (idx !== -1) aziende[idx].hasDipendenti = true;
			}

			showDipendenteModal = false;
			formDipendente = { nome: '', cognome: '', codiceFiscale: '', email: '', password: '' };
		}
		isSavingDipendente = false;
	}

	function apriModalUploadNuovo() {
		idDocumentoDaAggiornare = null;
		formDocumento = { modulo: ModuloServizio.SICUREZZA, tipologia: TipoDocumento.DVR, dataRilascio: '', dataScadenza: '' };
		showUploadModal = true;
	}

	function preparaAggiornamento(idDocumento: number | string) {
		const doc = documentiCorrenti.find(d => d.idDocumento === idDocumento);
		if(!doc) return;

		idDocumentoDaAggiornare = doc.idDocumento;
		formDocumento.modulo = doc.modulo;
		formDocumento.tipologia = doc.tipologia as TipoDocumento;
		formDocumento.dataRilascio = '';
		formDocumento.dataScadenza = '';
		showUploadModal = true;
	}

	async function gestisciUpload() {
		if (!uploadFile || !selectedAzienda || !formDocumento.dataScadenza || !formDocumento.dataRilascio) return;
		isUploading = true;

		const res = await uploadDocumentoUniversale(selectedAzienda.idUtente, uploadFile, formDocumento);

		if (res.error) {
			alert(res.msg);
		} else {
			if (idDocumentoDaAggiornare !== null) {
				await eliminaDocumentoUniversale(idDocumentoDaAggiornare);
			}
			documentiCorrenti = await DocumentoService.getDocumentiByAzienda(selectedAzienda.idUtente);
			showUploadModal = false;
			uploadFile = null;
			idDocumentoDaAggiornare = null;
		}
		isUploading = false;
	}

	async function scaricaDoc(idDocumento: number | string) {
		const doc = documentiCorrenti.find(d => d.idDocumento === idDocumento);
		if (!doc) return;
		await scaricaDocumentoUniversale(doc.idDocumento, doc.filePath);
	}

	function apriModaleRegistrazione() {
		isEditing = false;
		formAzienda = { idUtente: null, email: '', password: '', ragioneSociale: '', partitaIva: '', sedeLegale: '', pec: '', telefono: '', cellulare: '', referenteAziendale: '', hasDipendenti: false };
		showModal = true;
	}

	function apriModaleModifica() {
		if (!selectedAzienda) return;
		isEditing = true;
		formAzienda = {
			idUtente: selectedAzienda.idUtente,
			email: selectedAzienda.email,
			password: '',
			ragioneSociale: selectedAzienda.ragioneSociale,
			partitaIva: selectedAzienda.partitaIva,
			sedeLegale: selectedAzienda.sedeLegale || '',
			pec: selectedAzienda.pec || '',
			telefono: selectedAzienda.telefono || '',
			cellulare: selectedAzienda.cellulare || '',
			referenteAziendale: selectedAzienda.referenteAziendale || '',
			hasDipendenti: dynamicHasDipendenti
		};
		showModal = true;
	}

	async function salvaAzienda() {
		if (!isFormValid || isSaving) return;
		isSaving = true;

		let res;
		if (isEditing && formAzienda.idUtente) {
			res = await aggiornaUtenteUniversale<AziendaData>('AZIENDA', formAzienda.idUtente, formAzienda);
		} else {
			res = await creaUtenteUniversale<AziendaData>('AZIENDA', formAzienda);
		}

		if (res.error) {
			alert(res.msg);
		} else {
			const freshRes = await AnagraficaService.getAllAziende();
			const data = freshRes as AziendaData[];
			aziende = data.map(item => new Azienda(item));

			await Promise.all(aziende.map(async (a, i) => {
				aziende[i].hasDipendenti = await AnagraficaService.hasDipendenti(a.idUtente);
			}));

			if (isEditing && res.data) {
				const hasDip = await AnagraficaService.hasDipendenti(res.data.idUtente);
				const az = new Azienda({...res.data, hasDipendenti: hasDip});
				if (selectedAzienda?.idUtente === az.idUtente) selectedAzienda = az;
			}

			showModal = false;
		}
		isSaving = false;
	}

	function preparaEliminazione(a: Azienda | null) { if (!a) return; aziendaDaEliminare = a; confermaTesto = ''; showDeleteModal = true; }

	async function confermaEliminazione() {
		if (isConfermaValida && aziendaDaEliminare) {
			const res = await eliminaUtenteUniversale('AZIENDA', aziendaDaEliminare.idUtente);

			if (res.error) {
				alert(res.msg);
			} else {
				aziende = aziende.filter(a => a.idUtente !== aziendaDaEliminare?.idUtente);
				showDeleteModal = false;
				selectedAzienda = null;
			}
		}
	}

	function creaDocInfo(doc: Documento): DocInfo {
		const info = getInfoScadenza(doc.dataScadenza);

		return {
			id: doc.idDocumento,
			titolo: doc.tipologia.replace(/_/g, ' '),
			sottotitolo: doc.modulo,
			stato: info.stato,
			dataScadenza: formattaDataScadenza(doc.dataScadenza)
		};
	}
</script>

<div in:fade class="max-w-7xl mx-auto p-6">
	{#if !selectedAzienda}
		<div class="mb-10 flex justify-between items-start">
			<div>
				<h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Anagrafiche Aziende</h1>
				<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Gestione centralizzata NorLan.</p>
			</div>
			<button onclick={apriModaleRegistrazione} class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3">
				<Plus size={18} /> Nuova Azienda
			</button>
		</div>

		<div class="mb-8 relative w-72 group">
			<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={16} />
			<input bind:value={searchQuery} type="text" placeholder="Filtra azienda..." class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase" />
		</div>

		{#if isLoading}
			<div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-[#1B4B6B]" /></div>
		{:else}
			<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-3 gap-8">
				{#each filteredAziende as a (a.idUtente)}
					<AziendaCard
							idUtente={a.idUtente}
							ragioneSociale={a.ragioneSociale}
							partitaIva={a.partitaIva}
							sedeLegale={a.sedeLegale}
							hasDipendenti={a.hasDipendenti}
							onView={() => apriDettaglio(a)}
							onDelete={() => preparaEliminazione(a)}
					/>
				{/each}
			</div>
		{/if}

	{:else}
		<div in:fade>
			<button onclick={() => (selectedAzienda = null)} class="flex items-center gap-2 text-[#1B4B6B] font-extrabold uppercase text-[10px] mb-8 hover:gap-3 transition-all"><ChevronLeft size={16} /> Torna all'elenco</button>

			<DettagliCard
					nome={selectedAzienda.ragioneSociale}
					cognome=""
					sottotitolo={dynamicHasDipendenti ? 'Azienda con Personale' : 'Ditta Individuale'}
					onEdit={apriModaleModifica}
					onMail={() => apreGmail(selectedAzienda?.email || '')}
					onContact={() => vaiInChat(selectedAzienda?.idUtente || '')}
					onDelete={() => preparaEliminazione(selectedAzienda)}
					items={[
                 { label: 'Partita IVA', value: selectedAzienda.partitaIva, icon: Building2, isMono: true },
                 { label: 'Email Accesso', value: selectedAzienda.email, icon: Mail },
                 { label: 'PEC Certificata', value: selectedAzienda.pec || 'N.D.', icon: Mail },
                 { label: 'Sede Legale', value: selectedAzienda.sedeLegale || 'N.D.', icon: MapPin },
                 { label: 'Telefono', value: selectedAzienda.telefono || 'N.D.', icon: Phone },
                 { label: 'Cellulare', value: selectedAzienda.cellulare || 'N.D.', icon: Phone },
                 { label: 'Referente', value: selectedAzienda.referenteAziendale || 'Non assegnato', icon: UserCheck }
             ]}
			/>

			<div in:slide class="space-y-8 mb-16 mt-12">
				<div class="flex items-center justify-between">
					<div class="flex items-center gap-4">
						<div class="p-3 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-2xl shadow-inner"><FileText size={24} /></div>
						<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter">Documentazione ({documentiAziendaliFiltrati.length})</h2>
					</div>
					<button onclick={apriModalUploadNuovo} class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-6 py-2.5 rounded-xl font-extrabold uppercase text-[10px] flex items-center gap-2 hover:bg-[#1B4B6B] hover:text-white transition-all shadow-md">
						<Plus size={16} /> Carica Documento
					</button>
				</div>

				{#if documentiAziendaliFiltrati.length > 0}
					<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
						{#each documentiAziendaliFiltrati as doc (doc.idDocumento)}
							<div in:scale={{duration: 200}}>
								<DocCard
										documento={creaDocInfo(doc)}
										ruolo="admin"
										onDownload={() => scaricaDoc(doc.idDocumento)}
										onDelete={() => preparaEliminaDoc(doc)}
										onManage={getInfoScadenza(doc.dataScadenza).stato === 'DANGER'
										? () => preparaAggiornamento(doc.idDocumento)
									: undefined}
								/>
							</div>
						{/each}
					</div>
				{:else}
					<div class="bg-white rounded-3xl p-10 text-center border-2 border-dashed border-gray-200 text-gray-300 uppercase font-bold text-xs italic">Nessun documento caricato</div>
				{/if}
			</div>

			<div in:slide class="space-y-8 mb-20">
				<div class="flex items-center justify-between">
					<div class="flex items-center gap-4">
						<div class="p-3 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-2xl shadow-inner"><Users size={24} /></div>
						<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter">Personale ({dipendentiCorrenti.length})</h2>
					</div>
					<button onclick={() => (showDipendenteModal = true)} class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-6 py-2.5 rounded-xl font-extrabold uppercase text-[10px] flex items-center gap-2 hover:bg-[#1B4B6B] hover:text-white transition-all shadow-md">
						<UserPlus size={16} /> Aggiungi Dipendente
					</button>
				</div>

				{#if dipendentiCorrenti.length > 0}
					<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
						{#each dipendentiCorrenti as d (d.idUtente)}
							<DipendenteCard
									idUtente={d.idUtente}
									nome={d.nome}
									cognome={d.cognome}
									codiceFiscale={d.codiceFiscale}
									azienda={selectedAzienda?.ragioneSociale}
									ruolo="Dipendente"
									canEdit={false}
									canDelete={true}
									canContact={false}
									canViewDetails={true}
									onDelete={() => preparaEliminaDip(d)}
									onViewDetails={() => vaiADettaglioDipendente(d.idUtente)}
							/>
						{/each}
					</div>
				{:else}
					<div class="bg-white rounded-3xl p-10 text-center border-2 border-dashed border-gray-200 text-gray-300 uppercase font-bold text-xs italic">Nessun dipendente registrato</div>
				{/if}
			</div>
		</div>
	{/if}

	<ModalCard bind:isOpen={showDipendenteModal} maxWidth="max-w-2xl">
		{#snippet title()}
			<UserPlus size={20}/> <span class="font-black uppercase tracking-tighter">Registra Dipendente</span>
		{/snippet}

		<div class="space-y-4">
			<div class="grid grid-cols-2 gap-4">
				<div class="space-y-1">
					<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Nome *</label>
					<input bind:value={formDipendente.nome} placeholder="Es: Mario" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
				</div>
				<div class="space-y-1">
					<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Cognome *</label>
					<input bind:value={formDipendente.cognome} placeholder="Es: Rossi" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
				</div>
			</div>

			<div class="grid grid-cols-2 gap-4">
				<div class="space-y-1">
					<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Codice Fiscale *</label>
					<input bind:value={formDipendente.codiceFiscale} maxlength="16" placeholder="RSSMRA..." class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-mono focus:ring-2 focus:ring-[#1B4B6B] outline-none uppercase" />
				</div>
				<div class="space-y-1">
					<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Email Accesso *</label>
					<input bind:value={formDipendente.email} type="email" placeholder="m.rossi@email.it" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
				</div>
			</div>

			<div class="space-y-1">
				<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase tracking-tight">Password Temporanea *</label>
				<input bind:value={formDipendente.password} type="password" placeholder="••••••••" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
				<div class="mt-2 flex items-start gap-2">
					<div class="mt-0.5 text-orange-500"><AlertTriangle size={12}/></div>
					<p class="text-[8px] text-gray-400 font-bold uppercase leading-tight">
						Nota: Il lavoratore dovrà obbligatoriamente cambiare questa password al suo primo accesso.
					</p>
				</div>
			</div>
		</div>

		{#snippet footer()}
			<button onclick={() => (showDipendenteModal = false)} class="flex-1 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
			<button onclick={salvaNuovoDipendente} disabled={!isDipendenteValid || isSavingDipendente} class="flex-1 bg-[#1B4B6B] text-white py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center justify-center gap-2 hover:bg-[#153a54] transition-colors">
				{#if isSavingDipendente}<Loader2 size={14} class="animate-spin" />{/if} {isSavingDipendente ? 'Salvataggio...' : 'Conferma e Salva'}
			</button>
		{/snippet}
	</ModalCard>

	<ModalCard bind:isOpen={showUploadModal} maxWidth="max-w-lg">
		{#snippet title()}
			<FileText size={20}/> <span class="font-black uppercase tracking-tighter">Carica Documento</span>
		{/snippet}

		<div class="space-y-6">
			<div class="grid grid-cols-2 gap-4">
				<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Modulo</label><select bind:value={formDocumento.modulo} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold uppercase focus:ring-[#1B4B6B]">{#each Object.values(ModuloServizio) as mod (mod)}<option value={mod}>{mod}</option>{/each}</select></div>
				<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Tipologia</label><select bind:value={formDocumento.tipologia} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold uppercase focus:ring-[#1B4B6B]">{#each Object.values(TipoDocumento) as tipo (tipo)}<option value={tipo}>{tipo.replace(/_/g, ' ')}</option>{/each}</select></div>
			</div>
			<div class="grid grid-cols-2 gap-4">
				<div>
					<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Data di Rilascio *</label>
					<input type="date" max="9999-12-31" bind:value={formDocumento.dataRilascio} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
				</div>
				<div>
					<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Data Scadenza *</label>
					<input type="date" max="9999-12-31" bind:value={formDocumento.dataScadenza} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
				</div>
			</div>
			<div class="border-2 border-dashed border-gray-100 rounded-3xl p-8 text-center bg-gray-50/50 group">
				<input type="file" accept=".pdf,.doc,.docx" id="fileUpload" class="hidden" onchange={(e) => uploadFile = e.currentTarget.files?.[0] || null} />
				<label for="fileUpload" class="cursor-pointer flex flex-col items-center gap-3">
					<div class="p-4 bg-white rounded-full text-[#1B4B6B] shadow-sm group-hover:scale-110 transition-transform"><Upload size={24} /></div>
					<span class="text-xs font-black text-gray-500 uppercase tracking-tighter">{uploadFile ? uploadFile.name : 'Seleziona file'}</span>
				</label>
			</div>
		</div>

		{#snippet footer()}
			<button onclick={() => (showUploadModal = false)} class="flex-1 py-3 text-[10px] font-black uppercase text-gray-400">Annulla</button>
			<button onclick={gestisciUpload} disabled={isUploading || !uploadFile || !formDocumento.dataScadenza || !formDocumento.dataRilascio} class="flex-1 bg-[#1B4B6B] text-white py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center justify-center gap-2 hover:bg-[#153a54]">
				{#if isUploading}<Loader2 size={14} class="animate-spin" />{/if} {isUploading ? 'Salvataggio...' : 'Conferma'}
			</button>
		{/snippet}
	</ModalCard>

	<ModalCard bind:isOpen={showModal} maxWidth="max-w-2xl">
		{#snippet title()}
			{#if isEditing}
				<Edit3 size={20}/> <span class="font-black uppercase tracking-tighter">Modifica Dati Azienda</span>
			{:else}
				<Building2 size={20}/> <span class="font-black uppercase tracking-tighter">Nuova Anagrafica Aziendale</span>
			{/if}
		{/snippet}

		<div class="grid grid-cols-1 md:grid-cols-2 gap-x-8 gap-y-6">
			<div class="col-span-full mb-2">
				<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-3">Tipo di Organizzazione *</label>
				<div class="grid grid-cols-2 gap-4">
					<button type="button" onclick={() => formAzienda.hasDipendenti = false} class="flex items-center justify-center gap-3 p-4 rounded-2xl border-2 transition-all {!formAzienda.hasDipendenti ? 'border-[#1B4B6B] bg-[#1B4B6B]/5' : 'border-gray-100 hover:border-gray-200'}">
						<User size={20} class={!formAzienda.hasDipendenti ? 'text-[#1B4B6B]' : 'text-gray-400'} />
						<span class="text-[10px] font-black uppercase {!formAzienda.hasDipendenti ? 'text-[#1B4B6B]' : 'text-gray-400'}">Ditta Individuale</span>
					</button>
					<button type="button" onclick={() => formAzienda.hasDipendenti = true} class="flex items-center justify-center gap-3 p-4 rounded-2xl border-2 transition-all {formAzienda.hasDipendenti ? 'border-[#1B4B6B] bg-[#1B4B6B]/5' : 'border-gray-100 hover:border-gray-200'}">
						<Users size={20} class={formAzienda.hasDipendenti ? 'text-[#1B4B6B]' : 'text-gray-400'} />
						<span class="text-[10px] font-black uppercase {formAzienda.hasDipendenti ? 'text-[#1B4B6B]' : 'text-gray-400'}">Azienda con Personale</span>
					</button>
				</div>
			</div>
			<div class="space-y-4">
				<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Ragione Sociale *</label><input bind:value={formAzienda.ragioneSociale} placeholder="Es: Norlan Srl" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
				<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Partita IVA (11 cifre) *</label><input bind:value={formAzienda.partitaIva} maxlength="11" placeholder="01234567890" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
				<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">PEC Certificata</label><input bind:value={formAzienda.pec} placeholder="azienda@pec.it" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
				<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Sede Legale</label><input bind:value={formAzienda.sedeLegale} placeholder="Indirizzo completo" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
			</div>
			<div class="space-y-4">
				<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Email Accesso *</label><input bind:value={formAzienda.email} type="email" placeholder="admin@azienda.it" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>

				{#if !isEditing}
					<div>
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Password *</label>
						<input bind:value={formAzienda.password} type="password" placeholder="••••••••" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" />
					</div>
				{:else}
					<div class="p-4 bg-blue-50 border border-blue-100 rounded-xl">
						<p class="text-[10px] font-bold text-blue-800 uppercase leading-tight">Nota di Sicurezza</p>
						<p class="text-[9px] text-blue-600 uppercase mt-1 leading-relaxed">Per motivi di sicurezza, la password può essere modificata solo dal cliente tramite l'apposita procedura.</p>
					</div>
				{/if}

				<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Referente Aziendale</label><input bind:value={formAzienda.referenteAziendale} placeholder="Nome e Cognome" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
				<div class="grid grid-cols-2 gap-3">
					<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Telefono</label><input bind:value={formAzienda.telefono} placeholder="Fisso" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
					<div><label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Cellulare</label><input bind:value={formAzienda.cellulare} placeholder="Mobile" class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-[#1B4B6B]" /></div>
				</div>
			</div>
		</div>

		{#snippet footer()}
			<button onclick={() => (showModal = false)} class="flex-1 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
			<button onclick={salvaAzienda} disabled={!isFormValid || isSaving} class="flex-1 bg-[#1B4B6B] text-white py-3 rounded-xl text-[10px] font-black uppercase shadow-lg flex items-center justify-center gap-2 hover:bg-[#153a54]">
				{#if isSaving}
					<Loader2 size={14} class="animate-spin"/>
				{:else if isEditing}
					<Edit3 size={14}/>
				{:else}
					<Plus size={14}/>
				{/if}
				{isEditing ? 'Aggiorna Dati' : 'Salva Azienda'}
			</button>
		{/snippet}
	</ModalCard>

	<ModalCard bind:isOpen={showDeleteModal} maxWidth="max-w-md" headerClass="bg-red-600">
		{#snippet title()}
			<AlertTriangle size={20}/> <span class="font-black uppercase tracking-tighter">Eliminazione Azienda</span>
		{/snippet}

		<div class="text-center py-4">
			<div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><AlertTriangle size={40}/></div>
			<p class="text-sm text-gray-400 mb-8">Questa azione è irreversibile. Digita <span class="font-bold text-red-600">ELIMINA</span> per confermare la cancellazione di <span class="font-bold text-[#1B4B6B]">{aziendaDaEliminare?.ragioneSociale}</span>.</p>

			<div class="relative w-full group">
				<input
						bind:value={confermaTesto}
						maxlength="7"
						class="absolute inset-0 w-full h-full opacity-0 z-10 cursor-text uppercase"
				/>
				<div class="w-full p-4 bg-gray-50 border-2 border-transparent group-focus-within:border-red-600 rounded-2xl text-center font-black uppercase transition-all flex justify-center items-center text-2xl tracking-[0.3em] pl-[0.3em]">
					{#each 'ELIMINA'.split('') as char, i (i)}
                        <span class={confermaTesto.length > i ? (confermaTesto.toUpperCase()[i] === char ? 'text-red-600' : 'text-orange-600') : 'text-gray-300 transition-colors'}>
                        {confermaTesto.toUpperCase()[i] || char}
                        </span>
					{/each}
				</div>
			</div>
		</div>

		{#snippet footer()}
			<div class="flex flex-col w-full gap-3">
				<button onclick={confermaEliminazione} disabled={!isConfermaValida} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 disabled:opacity-30 disabled:shadow-none transition-all hover:bg-red-700">Sì, elimina definitivamente</button>
				<button onclick={() => { showDeleteModal = false; confermaTesto = ''; }} class="w-full py-2 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">No, annulla l'operazione</button>
			</div>
		{/snippet}
	</ModalCard>

	<ModalCard bind:isOpen={showDeleteDocModal} maxWidth="max-w-md" headerClass="bg-red-600">
		{#snippet title()}
			<Trash2 size={20}/> <span class="font-black uppercase tracking-tighter">Eliminazione Documento</span>
		{/snippet}

		<div class="text-center py-4">
			<div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><Trash2 size={40}/></div>
			<p class="text-sm text-gray-400">Stai per rimuovere definitivamente: <br><span class="font-bold text-[#1B4B6B]">{docDaEliminare?.tipologia.replace(/_/g, ' ')}</span></p>
		</div>

		{#snippet footer()}
			<div class="flex flex-col w-full gap-3">
				<button onclick={confermaEliminaDoc} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">Conferma Eliminazione</button>
				<button onclick={() => (showDeleteDocModal = false)} class="w-full py-2 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">Annulla</button>
			</div>
		{/snippet}
	</ModalCard>

	<ModalCard bind:isOpen={showDeleteDipModal} maxWidth="max-w-md" headerClass="bg-red-600">
		{#snippet title()}
			<Users size={20}/> <span class="font-black uppercase tracking-tighter">Rimuovi Dipendente</span>
		{/snippet}

		<div class="text-center py-4">
			<div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6"><Users size={40}/></div>
			<p class="text-sm text-gray-400">Il lavoratore <span class="font-bold text-[#1B4B6B]">{dipDaEliminare?.nome} {dipDaEliminare?.cognome}</span> verrà rimosso dall'anagrafica aziendale.</p>
		</div>

		{#snippet footer()}
			<div class="flex flex-col w-full gap-3">
				<button onclick={confermaEliminaDip} class="w-full bg-red-600 text-white py-4 rounded-2xl font-black uppercase text-[10px] shadow-lg shadow-red-200 transition-all hover:bg-red-700">Rimuovi Dipendente</button>
				<button onclick={() => (showDeleteDipModal = false)} class="w-full py-2 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600">Annulla</button>
			</div>
		{/snippet}
	</ModalCard>
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>