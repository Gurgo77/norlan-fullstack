<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Building2, Plus, X, Trash2,
		ShieldCheck, ChevronRight, ChevronLeft, Loader2, Search, AlertTriangle,
		MapPin, Mail, Phone, User, Globe, Users, UserCheck, Lock
	} from 'lucide-svelte';

	// Modelli
	import { Azienda } from '$lib/models/Azienda';
	import { Dipendente } from '$lib/models/Dipendente';
	import { Ruolo } from '$lib/models/Enums';

	// Servizi
	import { AziendaService } from '$lib/services/AziendaService';
	import { DipendenteService } from '$lib/services/DipendenteService';
	import { isAxiosError } from 'axios';

	// --- STATO REATTIVO (Svelte 5) ---
	let aziende = $state<Azienda[]>([]);
	let dipendentiAll = $state<Dipendente[]>([]);
	let isLoading = $state(true);
	let searchQuery = $state('');

	let selectedAzienda = $state<Azienda | null>(null);
	let showModal = $state(false);
	let isSaving = $state(false);

	// Stato del modulo per la nuova azienda
	let formAzienda = $state({
		ragioneSociale: '',
		partitaIva: '',
		email: '',
		password: '',
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

	// --- LOGICA DERIVATA ---
	const isFormValid = $derived(
			formAzienda.ragioneSociale.trim() !== '' &&
			formAzienda.partitaIva.length === 11 &&
			formAzienda.email.trim() !== '' &&
			formAzienda.password.trim() !== ''
	);

	const filteredAziende = $derived(
			aziende.filter(a => a.ragioneSociale.toLowerCase().includes(searchQuery.toLowerCase()))
	);

	// FIX: Usiamo (d as any) per evitare l'errore di TypeScript su idAzienda se non è esplicito nel modello
	const dipendentiCorrenti = $derived(
			selectedAzienda
					? dipendentiAll.filter(d => (d as any).idAzienda === selectedAzienda?.idUtente)
					: []
	);

	// --- AZIONI ---
	onMount(async () => {
		try {
			const [resAziende, resDipendenti] = await Promise.all([
				AziendaService.getAll(),
				DipendenteService.getAll().catch(() => [])
			]);

			aziende = resAziende;
			dipendentiAll = resDipendenti;
		} catch (error) {
			console.error("Errore caricamento dati:", error);
			if (isAxiosError(error) && error.code === 'ERR_NETWORK') {
				alert("Errore: Il backend (localhost:8080) è offline.");
			}
		} finally {
			isLoading = false;
		}
	});

	async function salvaNuovaAzienda() {
		if (!isFormValid) return;
		isSaving = true;

		try {
			// Payload completo con Ruolo (risolve l'errore di interfaccia)
			const payload = {
				...formAzienda,
				ruolo: Ruolo.AZIENDA
			};

			const nuova = await AziendaService.create(payload);
			aziende = [...aziende, nuova];

			// Reset Form
			formAzienda = { ragioneSociale: '', partitaIva: '', email: '', password: '', sedeLegale: '', pec: '', telefono: '', cellulare: '', referenteAziendale: '', hasDipendenti: false };
			showModal = false;
		} catch (error) {
			console.error("Errore salvataggio:", error);
			alert("Errore durante il salvataggio dell'azienda.");
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
				await AziendaService.delete(aziendaDaEliminare.idUtente);
				aziende = aziende.filter(a => a.idUtente !== aziendaDaEliminare?.idUtente);
				showDeleteModal = false;
				aziendaDaEliminare = null;
				selectedAzienda = null;
			} catch (error) {
				console.error("Errore eliminazione:", error);
				alert("Errore durante l'eliminazione.");
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
						<div role="button" tabindex="0" onclick={() => selectedAzienda = a} onkeydown={(e) => e.key === 'Enter' && (selectedAzienda = a)} class="p-8 pb-4 cursor-pointer flex-1">
							<div class="flex justify-between items-start mb-6">
								<div class="p-4 bg-gray-50 rounded-2xl text-[#1B4B6B] group-hover:bg-[#1B4B6B] group-hover:text-white transition-all"><Building2 size={28} /></div>
								<button onclick={(e) => { e.stopPropagation(); preparaEliminazione(a); }} class="text-gray-300 hover:text-red-600 transition-colors p-2 rounded-lg hover:bg-red-50 z-10"><Trash2 size={20} /></button>
							</div>
							<h3 class="font-extrabold text-[#1B4B6B] text-xl mb-1 uppercase truncate">{a.ragioneSociale}</h3>
							<div class="flex items-center gap-2 mb-4">
								<p class="text-[10px] text-gray-400 font-bold uppercase">P.IVA: {a.partitaIva}</p>
								<span class="text-[8px] font-black px-2 py-0.5 rounded border {a.hasDipendenti ? 'border-purple-100 bg-purple-50 text-purple-600' : 'border-blue-100 bg-blue-50 text-blue-600'} uppercase">{a.hasDipendenti ? 'Con Personale' : 'Individuale'}</span>
							</div>
						</div>
						<button onclick={() => selectedAzienda = a} class="mt-auto w-full p-6 pt-4 border-t border-gray-50 flex justify-between items-center hover:bg-gray-50/50 transition-colors">
							<div class="flex items-center gap-2"><ShieldCheck size={16} class="text-green-600"/><span class="text-[10px] font-bold text-gray-400 uppercase italic">Verificata</span></div>
							<ChevronRight size={20} class="text-[#1B4B6B]" />
						</button>
					</div>
				{/each}
			</div>
		{/if}
	{:else}
		<div in:fade>
			<button onclick={() => selectedAzienda = null} class="flex items-center gap-2 text-[#1B4B6B] font-extrabold uppercase text-[10px] mb-8 hover:gap-3 transition-all"><ChevronLeft size={16} /> Torna indietro</button>
			<div class="bg-white rounded-3xl shadow-xl border border-gray-100 overflow-hidden mb-8">
				<div class="bg-[#1B4B6B] p-10 text-white flex justify-between items-end relative overflow-hidden">
					<div class="relative z-10">
						<div class="flex items-center gap-3 mb-4">
							<span class="bg-green-500 text-white text-[10px] font-black px-3 py-1 rounded-full uppercase">Attiva</span>
							<span class="bg-white/10 text-white text-[10px] font-black px-3 py-1 rounded-full border border-white/20 uppercase">{selectedAzienda.hasDipendenti ? 'Con Dipendenti' : 'Ditta Individuale'}</span>
						</div>
						<h1 class="text-4xl font-extrabold uppercase tracking-tight">{selectedAzienda.ragioneSociale}</h1>
						<p class="text-white/40 font-bold uppercase text-[10px] mt-2 tracking-widest italic">Anagrafica Certificata NorLan</p>
					</div>
					<button onclick={() => preparaEliminazione(selectedAzienda)} class="relative z-10 flex items-center gap-2 bg-red-500/10 hover:bg-red-500 text-red-400 hover:text-white px-6 py-3 rounded-xl transition-all font-extrabold uppercase text-[10px] border border-red-500/20"><Trash2 size={16} /> Elimina</button>
				</div>
				<div class="p-10 grid grid-cols-1 lg:grid-cols-2 gap-12">
					<div class="space-y-8">
						<h2 class="text-[#1B4B6B] font-extrabold uppercase text-xs border-b pb-4 flex items-center gap-2"><Globe size={16} /> Dati Legali</h2>
						<div class="grid grid-cols-1 gap-6">
							<div><p class="text-[9px] font-bold text-gray-400 uppercase">Partita IVA</p><p class="text-sm font-extrabold text-[#1B4B6B] tracking-widest">{selectedAzienda.partitaIva}</p></div>
							<div><p class="text-[9px] font-bold text-gray-400 uppercase flex items-center gap-1"><MapPin size={10}/> Sede Legale</p><p class="text-sm font-bold text-[#1B4B6B] uppercase">{selectedAzienda.sedeLegale || 'N.D.'}</p></div>
						</div>
					</div>
					<div class="space-y-8">
						<h2 class="text-[#1B4B6B] font-extrabold uppercase text-xs border-b pb-4 flex items-center gap-2"><User size={16} /> Contatti</h2>
						<div class="grid grid-cols-1 gap-6">
							<div><p class="text-[9px] font-bold text-gray-400 uppercase">Referente</p><p class="text-sm font-extrabold text-[#1B4B6B] uppercase">{selectedAzienda.referenteAziendale || '-'}</p></div>
							<div class="flex gap-8">
								<div><p class="text-[9px] font-bold text-gray-400 uppercase flex items-center gap-1"><Mail size={10}/> PEC</p><p class="text-sm font-bold text-[#1B4B6B]">{selectedAzienda.pec || '-'}</p></div>
								<div><p class="text-[9px] font-bold text-gray-400 uppercase flex items-center gap-1"><Phone size={10}/> Tel</p><p class="text-sm font-bold text-[#1B4B6B]">{selectedAzienda.telefono || '-'}</p></div>
								<div><p class="text-[9px] font-bold text-gray-400 uppercase flex items-center gap-1"><Phone size={10}/> Cel</p><p class="text-sm font-bold text-[#1B4B6B]">{selectedAzienda.cellulare || '-'}</p></div>
							</div>
						</div>
					</div>
				</div>
			</div>
		</div>
	{/if}
</div>

{#if showModal}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-[#1B4B6B]/80 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-3xl rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]">
			<div class="bg-[#1B4B6B] p-8 text-white flex justify-between items-center shrink-0">
				<div>
					<h2 class="text-2xl font-extrabold uppercase tracking-tight">Inserimento Anagrafica</h2>
					<p class="text-[10px] text-white/50 font-bold uppercase tracking-widest mt-1">Completa tutti i dati richiesti dal modello</p>
				</div>
				<button onclick={() => showModal = false} class="hover:bg-white/10 p-2 rounded-xl transition-colors"><X size={28} /></button>
			</div>

			<div class="p-8 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
				<div class="space-y-8">
					<div>
						<h3 class="text-xs font-black text-[#1B4B6B] uppercase border-b border-gray-100 pb-2 mb-4 flex items-center gap-2"><Building2 size={16}/> Struttura Organizzativa</h3>
						<div class="grid grid-cols-2 gap-4">
							<button onclick={() => formAzienda.hasDipendenti = false} class="p-4 rounded-2xl flex flex-col items-center gap-2 transition-all border-2 {formAzienda.hasDipendenti === false ? 'bg-white border-[#1B4B6B] text-[#1B4B6B] shadow-md' : 'bg-transparent border-gray-200 text-gray-400 hover:border-gray-300'}">
								<UserCheck size={24} />
								<span class="font-extrabold uppercase text-[10px]">Persona Singola</span>
							</button>
							<button onclick={() => formAzienda.hasDipendenti = true} class="p-4 rounded-2xl flex flex-col items-center gap-2 transition-all border-2 {formAzienda.hasDipendenti === true ? 'bg-white border-[#1B4B6B] text-[#1B4B6B] shadow-md' : 'bg-transparent border-gray-200 text-gray-400 hover:border-gray-300'}">
								<Users size={24} />
								<span class="font-extrabold uppercase text-[10px]">Con Dipendenti</span>
							</button>
						</div>
					</div>

					<div>
						<h3 class="text-xs font-black text-[#1B4B6B] uppercase border-b border-gray-100 pb-2 mb-4 flex items-center gap-2"><Globe size={16}/> Dati Legali</h3>
						<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
							<div class="space-y-1">
								<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Ragione Sociale *</label>
								<input bind:value={formAzienda.ragioneSociale} type="text" placeholder="Es. NorLan Srl" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl font-extrabold uppercase text-xs outline-none focus:ring-2 focus:ring-[#1B4B6B]" />
							</div>
							<div class="space-y-1">
								<label class="text-[10px] font-bold text-gray-400 uppercase ml-1 flex justify-between">Partita IVA * <span class="text-gray-300 font-normal">{formAzienda.partitaIva.length}/11</span></label>
								<input bind:value={formAzienda.partitaIva} type="text" maxlength="11" placeholder="11 cifre numeriche" class="w-full px-5 py-3 bg-white border {formAzienda.partitaIva.length > 0 && formAzienda.partitaIva.length !== 11 ? 'border-red-300 focus:ring-red-500 text-red-600' : 'border-gray-200 focus:ring-[#1B4B6B]'} rounded-2xl font-bold text-xs outline-none focus:ring-2" />
							</div>
							<div class="col-span-1 md:col-span-2 space-y-1">
								<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Sede Legale</label>
								<input bind:value={formAzienda.sedeLegale} type="text" placeholder="Via, Civico, CAP, Città (PR)" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl font-bold text-xs outline-none focus:ring-2 focus:ring-[#1B4B6B] uppercase" />
							</div>
						</div>
					</div>

					<div>
						<h3 class="text-xs font-black text-[#1B4B6B] uppercase border-b border-gray-100 pb-2 mb-4 flex items-center gap-2"><Phone size={16}/> Referente & Contatti</h3>
						<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
							<div class="col-span-1 md:col-span-2 space-y-1">
								<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Referente Aziendale</label>
								<input bind:value={formAzienda.referenteAziendale} type="text" placeholder="Nome e Cognome" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl font-bold text-xs outline-none focus:ring-2 focus:ring-[#1B4B6B] uppercase" />
							</div>
							<div class="space-y-1">
								<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Email PEC</label>
								<input bind:value={formAzienda.pec} type="email" placeholder="azienda@pec.it" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl font-bold text-xs outline-none focus:ring-2 focus:ring-[#1B4B6B] lowercase" />
							</div>
							<div class="space-y-1">
								<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Telefono Fisso</label>
								<input bind:value={formAzienda.telefono} type="text" placeholder="Es. 02 123456" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl font-bold text-xs outline-none focus:ring-2 focus:ring-[#1B4B6B]" />
							</div>
							<div class="space-y-1">
								<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Cellulare Referente</label>
								<input bind:value={formAzienda.cellulare} type="text" placeholder="Es. 333 1234567" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl font-bold text-xs outline-none focus:ring-2 focus:ring-[#1B4B6B]" />
							</div>
						</div>
					</div>

					<div>
						<h3 class="text-xs font-black text-[#1B4B6B] uppercase border-b border-gray-100 pb-2 mb-4 flex items-center gap-2"><Lock size={16}/> Accesso Portale</h3>
						<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
							<div class="space-y-1">
								<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Email Accesso *</label>
								<input bind:value={formAzienda.email} type="email" placeholder="admin@azienda.it" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl font-bold text-xs outline-none focus:ring-2 focus:ring-[#1B4B6B] lowercase" />
							</div>
							<div class="space-y-1">
								<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Password Iniziale *</label>
								<input bind:value={formAzienda.password} type="password" placeholder="••••••••" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl font-bold text-xs outline-none focus:ring-2 focus:ring-[#1B4B6B]" />
							</div>
						</div>
					</div>
				</div>
			</div>

			<div class="p-6 border-t border-gray-100 flex gap-4 bg-white items-center justify-between">
				<button onclick={() => showModal = false} class="px-8 py-4 border-2 border-gray-100 text-gray-400 font-extrabold rounded-2xl hover:bg-gray-50 transition-all uppercase text-xs">Annulla</button>
				<div class="flex items-center gap-4">
					{#if !isFormValid}
						<p class="text-[9px] font-bold text-red-500 uppercase tracking-widest text-right hidden md:block">Compila i campi obbligatori<br>e verifica P.IVA (11 cifre)</p>
					{/if}
					<button onclick={salvaNuovaAzienda} disabled={!isFormValid || isSaving} class="px-10 py-4 bg-[#1B4B6B] text-white font-extrabold rounded-2xl shadow-xl hover:bg-[#1B4B6B]/90 transition-all flex items-center justify-center gap-3 uppercase text-xs disabled:opacity-30 disabled:grayscale">
						{#if isSaving} <Loader2 size={18} class="animate-spin" /> Salvataggio... {:else} Salva Azienda {/if}
					</button>
				</div>
			</div>
		</div>
	</div>
{/if}

{#if showDeleteModal}
	<div class="fixed inset-0 z-[120] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-md rounded-3xl shadow-2xl overflow-hidden">
			<div class="bg-red-600 p-8 text-white text-center"><AlertTriangle size={48} class="mx-auto mb-4" /><h2 class="text-2xl font-extrabold uppercase">Attenzione</h2></div>
			<div class="p-8 text-center space-y-6">
				<p class="text-gray-500 font-bold text-sm uppercase">Digita <span class="text-red-600">ELIMINA</span> per confermare</p>
				<input bind:value={confermaTesto} type="text" class="w-full px-4 py-3 border border-red-100 rounded-xl text-center font-black text-red-600 uppercase tracking-widest outline-none focus:ring-2 focus:ring-red-600/20" placeholder="ELIMINA" />
				<div class="flex flex-col gap-3">
					<button onclick={confermaEliminazione} disabled={confermaTesto !== 'ELIMINA'} class="w-full py-4 bg-red-600 text-white font-extrabold rounded-2xl uppercase text-xs disabled:opacity-30">Conferma</button>
					<button onclick={() => showDeleteModal = false} class="text-gray-300 font-bold uppercase text-[10px]">Annulla</button>
				</div>
			</div>
		</div>
	</div>
{/if}

<style>
	.custom-scrollbar::-webkit-scrollbar { width: 6px; }
	.custom-scrollbar-thumb { background: #cbd5e1; border-radius: 10px; }
	.custom-scrollbar-track { background: transparent; }
</style>