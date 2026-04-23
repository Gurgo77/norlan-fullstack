<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale, slide } from 'svelte/transition';
	import {
		Building2, Plus, Trash2,
		ShieldCheck, ChevronRight, ChevronLeft, Loader2, Search, Phone, User, Globe, Users, UserCheck, MapPin
	} from 'lucide-svelte';

	// Modelli ed Enums
	import { Azienda } from '$lib/models/Azienda';
	import { Ruolo } from '$lib/models/Enums';

	// Servizi
	import { AziendaService, type AziendaCreateRequest } from '$lib/services/AziendaService';
	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';

	// --- STATO REATTIVO (Svelte 5) ---
	let aziende = $state<Azienda[]>([]);
	let dipendentiCorrenti = $state<DipendenteDTO[]>([]); // Tipizzazione forte
	let isLoading = $state(true);
	let searchQuery = $state('');

	let selectedAzienda = $state<Azienda | null>(null);
	let dynamicHasDipendenti = $state(false);
	let showModal = $state(false);
	let isSaving = $state(false);

	// Stato per il form di creazione
	let formAzienda = $state<AziendaCreateRequest>({
		email: '',
		password: '',
		ragioneSociale: '',
		partitaIva: '',
		sedeLegale: '',
		pec: '',
		telefono: '',
		cellulare: '',
		referenteAziendale: '',
		hasDipendenti: false,
		ruolo: Ruolo.AZIENDA
	});

	let showDeleteModal = $state(false);
	let aziendaDaEliminare = $state<Azienda | null>(null);
	let confermaTesto = $state('');

	// --- LOGICA DERIVATA ---
	const isFormValid = $derived(
			formAzienda.ragioneSociale.trim() !== '' &&
			formAzienda.partitaIva.length === 11 &&
			formAzienda.email.trim() !== '' &&
			(formAzienda.password ?? '').trim() !== ''
	);

	const filteredAziende = $derived(
			aziende.filter(a => a.ragioneSociale.toLowerCase().includes(searchQuery.toLowerCase()))
	);

	// --- AZIONI ---
	onMount(async () => {
		try {
			// Recupero iniziale di tutte le aziende tramite il service dedicato
			aziende = await AziendaService.getAll();
		} catch (error) {
			console.error("Errore caricamento dati aziende:", error);
		} finally {
			isLoading = false;
		}
	});

	/**
	 * Apre il dettaglio di un'azienda e carica i dipendenti associati on-demand.
	 */
	async function apriDettaglio(azienda: Azienda) {
		selectedAzienda = azienda;
		try {
			// Verifica dinamica se ha dipendenti
			dynamicHasDipendenti = await AziendaService.hasDipendenti(azienda.idUtente);

			if (dynamicHasDipendenti) {
				// Scarica solo i dipendenti di questa azienda
				dipendentiCorrenti = await LavoratoreService.getByAzienda(azienda.idUtente);
			} else {
				dipendentiCorrenti = [];
			}
		} catch (error) {
			console.error("Errore nel recupero dei dettagli:", error);
		}
	}

	async function salvaNuovaAzienda() {
		if (!isFormValid) return;
		isSaving = true;
		try {
			// Creazione tramite service
			const nuova = await AziendaService.create(formAzienda);
			aziende = [...aziende, nuova];
			showModal = false;
			// Reset form
			formAzienda = {
				email: '', password: '', ragioneSociale: '', partitaIva: '',
				sedeLegale: '', pec: '', telefono: '', cellulare: '',
				referenteAziendale: '', hasDipendenti: false, ruolo: Ruolo.AZIENDA
			};
		} catch (error) {
			console.error("Errore nel salvataggio:", error);
			alert("Errore durante la creazione dell'azienda.");
		} finally {
			isSaving = false;
		}
	}

	function preparaEliminazione(azienda: Azienda | null) {
		if (!azienda) return;
		aziendaDaEliminare = azienda;
		confermaTesto = '';
		showDeleteModal = true;
	}

	async function confermaEliminazione() {
		if (confermaTesto === 'ELIMINA' && aziendaDaEliminare) {
			try {
				// Eliminazione fisica tramite service
				await AziendaService.delete(aziendaDaEliminare.idUtente);
				aziende = aziende.filter(a => a.idUtente !== aziendaDaEliminare?.idUtente);
				showDeleteModal = false;
				selectedAzienda = null;
			} catch (error) {
				console.error("Errore eliminazione:", error);
				alert("Impossibile eliminare l'azienda.");
			}
		}
	}
</script>

<div in:fade>
	{#if !selectedAzienda}
		<div class="mb-10 flex justify-between items-start">
			<div>
				<h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Anagrafiche Aziende</h1>
				<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Gestione centralizzata NorLan.</p>
			</div>
			<button onclick={() => showModal = true} class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3">
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
			<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
				{#each filteredAziende as a (a.idUtente)}
					<div class="bg-white rounded-2xl shadow-sm border border-gray-100 hover:shadow-xl hover:border-[#1B4B6B]/20 hover:-translate-y-1 transition-all group relative flex flex-col h-full overflow-hidden" in:scale>
						<div role="button" tabindex="0" onclick={() => apriDettaglio(a)} onkeydown={(e) => e.key === 'Enter' && apriDettaglio(a)} class="p-8 pb-4 cursor-pointer flex-1">
							<div class="flex justify-between items-start mb-6">
								<div class="p-4 bg-gray-50 rounded-2xl text-[#1B4B6B] group-hover:bg-[#1B4B6B] group-hover:text-white transition-all"><Building2 size={28} /></div>
								<button onclick={(e) => { e.stopPropagation(); preparaEliminazione(a); }} class="text-gray-300 hover:text-red-600 transition-colors p-2 rounded-lg hover:bg-red-50 z-10"><Trash2 size={20} /></button>
							</div>
							<h3 class="font-extrabold text-[#1B4B6B] text-xl mb-1 uppercase truncate">{a.ragioneSociale}</h3>
							<div class="flex items-center gap-2 mb-4">
								<p class="text-[10px] text-gray-400 font-bold uppercase">P.IVA: {a.partitaIva}</p>
								<span class="text-[8px] font-black px-2 py-0.5 rounded border {a.hasDipendenti ? 'border-purple-100 bg-purple-50 text-purple-600' : 'border-blue-100 bg-blue-50 text-blue-600'} uppercase">
									{a.hasDipendenti ? 'Con Personale' : 'Individuale'}
								</span>
							</div>
						</div>
						<button onclick={() => apriDettaglio(a)} class="mt-auto w-full p-6 pt-4 border-t border-gray-50 flex justify-between items-center hover:bg-gray-50/50 transition-colors">
							<div class="flex items-center gap-2"><ShieldCheck size={16} class="text-green-600"/><span class="text-[10px] font-bold text-gray-400 uppercase italic">Verificata</span></div>
							<ChevronRight size={20} class="text-[#1B4B6B]" />
						</button>
					</div>
				{/each}
			</div>
		{/if}
	{:else}
		<div in:fade>
			<button onclick={() => selectedAzienda = null} class="flex items-center gap-2 text-[#1B4B6B] font-extrabold uppercase text-[10px] mb-8 hover:gap-3 transition-all"><ChevronLeft size={16} /> Torna all'elenco</button>

			<div class="bg-white rounded-3xl shadow-xl border border-gray-100 overflow-hidden mb-12">
				<div class="bg-[#1B4B6B] p-10 text-white flex justify-between items-end relative">
					<div>
						<div class="flex items-center gap-3 mb-4">
							{#if dynamicHasDipendenti}
								<span class="bg-purple-500 text-white text-[10px] font-black px-4 py-1.5 rounded-full uppercase flex items-center gap-2"><Users size={12}/> Azienda con Personale</span>
							{:else}
								<span class="bg-blue-500 text-white text-[10px] font-black px-4 py-1.5 rounded-full uppercase flex items-center gap-2"><User size={12}/> Ditta Individuale</span>
							{/if}
						</div>
						<h1 class="text-5xl font-extrabold uppercase tracking-tighter">{selectedAzienda.ragioneSociale}</h1>
					</div>
					<button onclick={() => preparaEliminazione(selectedAzienda)} class="flex items-center gap-2 bg-red-600 text-white px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] border border-red-500/20 shadow-xl"><Trash2 size={16} /> Elimina Anagrafica</button>
				</div>

				<div class="p-12 grid grid-cols-1 lg:grid-cols-3 gap-16 bg-gray-50/30">
					<div class="space-y-8">
						<h2 class="text-[#1B4B6B] font-black uppercase text-xs tracking-widest border-b border-gray-200 pb-4 flex items-center gap-2"><Globe size={16} /> Profilo Legale</h2>
						<div class="space-y-6">
							<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1">Partita IVA</p><p class="text-base font-extrabold text-[#1B4B6B] tracking-widest">{selectedAzienda.partitaIva}</p></div>
							<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1 flex items-center gap-1"><MapPin size={10}/> Sede Legale</p><p class="text-sm font-bold text-[#1B4B6B] uppercase leading-relaxed">{selectedAzienda.sedeLegale || 'N.D.'}</p></div>
						</div>
					</div>

					<div class="space-y-8">
						<h2 class="text-[#1B4B6B] font-black uppercase text-xs tracking-widest border-b border-gray-200 pb-4 flex items-center gap-2"><Phone size={16} /> Recapiti</h2>
						<div class="space-y-6">
							<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1">Email Accesso</p><p class="text-sm font-bold text-[#1B4B6B] lowercase">{selectedAzienda.email}</p></div>
							<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1">PEC Certificata</p><p class="text-sm font-bold text-[#1B4B6B] lowercase">{selectedAzienda.pec || '-'}</p></div>
							<div class="grid grid-cols-2 gap-4">
								<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1">Telefono</p><p class="text-sm font-bold text-[#1B4B6B]">{selectedAzienda.telefono || '-'}</p></div>
								<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1">Cellulare</p><p class="text-sm font-bold text-[#1B4B6B]">{selectedAzienda.cellulare || '-'}</p></div>
							</div>
						</div>
					</div>

					<div class="space-y-8">
						<h2 class="text-[#1B4B6B] font-black uppercase text-xs tracking-widest border-b border-gray-200 pb-4 flex items-center gap-2"><UserCheck size={16} /> Responsabile</h2>
						<div class="space-y-6">
							<div><p class="text-[10px] font-bold text-gray-400 uppercase mb-1">Referente Aziendale</p><p class="text-sm font-extrabold text-[#1B4B6B] uppercase">{selectedAzienda.referenteAziendale || 'Non assegnato'}</p></div>
						</div>
					</div>
				</div>
			</div>

			{#if dynamicHasDipendenti}
				<div in:slide class="space-y-8 mb-20">
					<div class="flex items-center gap-4">
						<div class="p-3 bg-purple-100 text-purple-600 rounded-2xl shadow-inner"><Users size={24} /></div>
						<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter">Personale Aziendale ({dipendentiCorrenti.length})</h2>
					</div>

					{#if dipendentiCorrenti.length > 0}
						<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
							{#each dipendentiCorrenti as d (d.idUtente)}
								<div class="bg-white p-6 rounded-3xl border border-gray-100 shadow-sm hover:shadow-xl transition-all">
									<div class="flex items-center gap-4 mb-4">
										<div class="w-12 h-12 bg-gray-50 rounded-2xl flex items-center justify-center text-[#1B4B6B] font-black text-sm">{d.nome[0]}{d.cognome[0]}</div>
										<h4 class="font-extrabold text-[#1B4B6B] uppercase text-sm leading-tight">{d.nome}<br>{d.cognome}</h4>
									</div>
									<div class="space-y-2 pt-4 border-t border-gray-50">
										<p class="text-[8px] text-gray-300 font-black uppercase">Codice Fiscale</p>
										<p class="text-[10px] font-mono font-bold text-gray-600">{d.codiceFiscale}</p>
									</div>
								</div>
							{/each}
						</div>
					{:else}
						<div class="bg-white rounded-3xl p-10 text-center border-2 border-dashed border-gray-200 text-gray-300 uppercase font-bold text-xs">Nessun dipendente censito</div>
					{/if}
				</div>
			{/if}
		</div>
	{/if}
</div>

{#if showModal}
	<div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-50 p-4" transition:fade>
		<div class="bg-white rounded-3xl shadow-2xl w-full max-w-3xl overflow-hidden flex flex-col max-h-[90vh]" transition:scale={{ start: 0.95 }}>

			<div class="p-8 border-b border-gray-100 flex justify-between items-center bg-gray-50/50 shrink-0">
				<div>
					<h3 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tighter">Registra Nuova Azienda</h3>
					<p class="text-xs font-bold text-gray-400 uppercase mt-1">Inserisci i dati per creare il profilo cliente</p>
				</div>
				<button onclick={() => showModal = false} class="text-gray-400 hover:text-red-500 font-bold text-xl transition-colors px-4 py-2">&times;</button>
			</div>

			<div class="p-8 overflow-y-auto custom-scrollbar-data grid grid-cols-1 md:grid-cols-2 gap-6">
				<div class="md:col-span-2 space-y-2">
					<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Ragione Sociale *</label>
					<input bind:value={formAzienda.ragioneSociale} type="text" class="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none font-bold uppercase" placeholder="Es. NorLan S.R.L.">
				</div>
				<div class="space-y-2">
					<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Partita IVA *</label>
					<input bind:value={formAzienda.partitaIva} type="text" maxlength="11" class="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none font-bold" placeholder="11 cifre">
				</div>
				<div class="space-y-2">
					<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Sede Legale</label>
					<input bind:value={formAzienda.sedeLegale} type="text" class="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none font-bold uppercase">
				</div>

				<div class="md:col-span-2 border-t border-gray-100 pt-6 mt-2">
					<h4 class="text-[#1B4B6B] font-black uppercase text-xs mb-4">Credenziali di Accesso</h4>
				</div>
				<div class="space-y-2">
					<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Email Accesso *</label>
					<input bind:value={formAzienda.email} type="email" class="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none font-bold lowercase" placeholder="email@azienda.it">
				</div>
				<div class="space-y-2">
					<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Password Temporanea *</label>
					<input bind:value={formAzienda.password} type="password" class="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none font-bold" placeholder="*******">
				</div>

				<div class="md:col-span-2 border-t border-gray-100 pt-6 mt-2">
					<h4 class="text-[#1B4B6B] font-black uppercase text-xs mb-4">Recapiti e Struttura</h4>
				</div>
				<div class="space-y-2">
					<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest">PEC</label>
					<input bind:value={formAzienda.pec} type="email" class="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none font-bold lowercase">
				</div>
				<div class="space-y-2">
					<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Referente Aziendale</label>
					<input bind:value={formAzienda.referenteAziendale} type="text" class="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none font-bold uppercase">
				</div>

				<div class="md:col-span-2 mt-4 p-4 bg-blue-50 border border-blue-100 rounded-xl flex items-center justify-between cursor-pointer" onclick={() => formAzienda.hasDipendenti = !formAzienda.hasDipendenti}>
					<div>
						<p class="font-bold text-[#1B4B6B] text-sm uppercase">Ha dipendenti a carico?</p>
						<p class="text-[10px] text-gray-500 font-bold">Attiva se l'azienda deve gestire lavoratori e DPI.</p>
					</div>
					<div class="w-12 h-6 bg-gray-300 rounded-full relative transition-colors duration-300 {formAzienda.hasDipendenti ? 'bg-green-500' : ''}">
						<div class="w-4 h-4 bg-white rounded-full absolute top-1 left-1 transition-transform duration-300 {formAzienda.hasDipendenti ? 'translate-x-6' : ''}"></div>
					</div>
				</div>
			</div>

			<div class="p-6 bg-gray-50 border-t border-gray-100 flex justify-end gap-4 shrink-0">
				<button onclick={() => showModal = false} class="px-6 py-3 font-bold text-gray-500 hover:text-gray-700 uppercase text-xs">Annulla</button>
				<button
						onclick={salvaNuovaAzienda}
						disabled={!isFormValid || isSaving}
						class="px-8 py-3 bg-[#1B4B6B] text-white rounded-xl font-black uppercase text-xs shadow-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-[#1B4B6B]/90 transition-all flex items-center gap-2"
				>
					{#if isSaving} <Loader2 size={16} class="animate-spin" /> Salvataggio... {:else} Registra Azienda {/if}
				</button>
			</div>
		</div>
	</div>
{/if}

{#if showDeleteModal && aziendaDaEliminare}
	<div class="fixed inset-0 bg-red-900/40 backdrop-blur-sm flex items-center justify-center z-50 p-4" transition:fade>
		<div class="bg-white rounded-3xl shadow-2xl w-full max-md overflow-hidden text-center" transition:scale={{ start: 0.95 }}>
			<div class="p-10">
				<div class="w-20 h-20 bg-red-50 text-red-600 rounded-full flex items-center justify-center mx-auto mb-6">
					<Trash2 size={32} />
				</div>
				<h3 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter mb-2">Sei sicuro?</h3>
				<p class="text-sm font-bold text-gray-500 mb-8">Stai per eliminare <span class="text-red-600"> {aziendaDaEliminare.ragioneSociale} </span>. L'operazione rimuoverà anche i dipendenti associati. Non è reversibile.</p>

				<div class="text-left space-y-2 mb-8">
					<label class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Digita "ELIMINA" per confermare</label>
					<input bind:value={confermaTesto} type="text" class="w-full px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-center text-sm focus:ring-2 focus:ring-red-500 outline-none font-black uppercase text-red-600">
				</div>

				<div class="flex gap-4">
					<button onclick={() => showDeleteModal = false} class="flex-1 py-3.5 bg-gray-100 text-gray-600 rounded-xl font-bold uppercase text-xs hover:bg-gray-200 transition-colors">Annulla</button>
					<button onclick={confermaEliminazione} disabled={confermaTesto !== 'ELIMINA'} class="flex-1 py-3.5 bg-red-600 text-white rounded-xl font-black uppercase text-xs shadow-md disabled:opacity-50 disabled:cursor-not-allowed hover:bg-red-700 transition-colors">Conferma</button>
				</div>
			</div>
		</div>
	</div>
{/if}