
<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		GraduationCap, Plus, X, Trash2, Search,
		Calendar, MapPin, Users, BookOpen, Loader2
	} from 'lucide-svelte';

	// Modelli
	import { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { Docente, type DocenteData } from '$lib/models/Docente';
	import { StatoCorso } from '$lib/models/Enums';
	import type { CorsoFormazioneRequest } from '$lib/models/CorsoFormazioneRequest';

	// Servizi
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';

	// --- STATO REATTIVO ---
	let corsi = $state<CorsoFormazione[]>([]);
	let docenti = $state<Docente[]>([]);
	let isLoading = $state(true);
	let isSaving = $state(false);
	let searchQuery = $state('');
	let showModal = $state(false);

	// Form di creazione tipizzato
	let formCorso = $state<Partial<CorsoFormazioneRequest>>({
		titolo: '',
		dataOrario: '',
		luogoFisico: '',
		capacitaMassima: 20,
		idDocente: undefined
	});

	// Validazione derivata
	const isFormValid = $derived(
			!!formCorso.titolo &&
			!!formCorso.dataOrario &&
			!!formCorso.luogoFisico &&
			!!formCorso.idDocente &&
			(formCorso.capacitaMassima ?? 0) > 0
	);

	const filteredCorsi = $derived(
			corsi.filter(c => c.titolo.toLowerCase().includes(searchQuery.toLowerCase()))
	);

	// --- AZIONI ---
	onMount(async () => {
		try {
			// Fetch parallelo di corsi e docenti per massimizzare le prestazioni
			const [corsiResponse, docentiResponse] = await Promise.all([
				FormazioneService.getAllCorsi(),
				AnagraficaService.getAllDocenti()
			]);

			corsi = corsiResponse;
			// Eseguiamo il cast per mappare i dati unknown ritornati da AnagraficaService
			const docentiRaw = docentiResponse as DocenteData[];
			docenti = docentiRaw.map(d => new Docente(d));

		} catch (error) {
			console.error("Errore nel caricamento della formazione:", error);
		} finally {
			isLoading = false;
		}
	});

	async function salvaNuovoCorso() {
		if (!isFormValid) return;
		isSaving = true;

		try {
			// Formattiamo la data per il backend (ISO string)
			const isoDate = new Date(formCorso.dataOrario!).toISOString();

			const payload: CorsoFormazioneRequest = {
				titolo: formCorso.titolo!,
				dataOrario: isoDate,
				luogoFisico: formCorso.luogoFisico!,
				capacitaMassima: formCorso.capacitaMassima!,
				idDocente: formCorso.idDocente!
			};

			const nuovoCorso = await FormazioneService.createCorso(payload);
			corsi = [...corsi, nuovoCorso];

			showModal = false;

			// Reset Form
			formCorso = {
				titolo: '',
				dataOrario: '',
				luogoFisico: '',
				capacitaMassima: 20,
				idDocente: undefined
			};
		} catch (error) {
			console.error("Errore durante la creazione del corso:", error);
			alert("Impossibile programmare il corso. Verifica i dati inseriti.");
		} finally {
			isSaving = false;
		}
	}

	async function eliminaCorso(idCorso: number) {
		if (!confirm("Sei sicuro di voler annullare e cancellare questo corso?")) return;

		try {
			await FormazioneService.deleteCorso(idCorso);
			corsi = corsi.filter(c => c.idCorso !== idCorso);
		} catch (error) {
			console.error("Errore durante l'eliminazione del corso:", error);
			alert("Errore durante l'eliminazione del corso.");
		}
	}

	function formattaData(isoString: string) {
		if (!isoString) return '';
		const d = new Date(isoString);
		return d.toLocaleDateString('it-IT', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute:'2-digit' });
	}
</script>

<div in:fade>
	<div class="mb-10 flex justify-between items-start">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B]">GESTIONE FORMAZIONE</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Pianificazione corsi, aule e docenze.</p>
		</div>

		<button
				onclick={() => showModal = true}
				class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3"
		>
			<Plus size={18} />
			Programma Corso
		</button>
	</div>

	<div class="mb-8 relative w-72 group">
		<Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={16} />
		<input
				bind:value={searchQuery}
				type="text"
				placeholder="Cerca per titolo..."
				class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:ring-2 focus:ring-[#1B4B6B] outline-none transition-all font-bold uppercase"
		/>
	</div>

	{#if isLoading}
		<div class="py-20 text-center"><Loader2 size={40} class="animate-spin mx-auto text-[#1B4B6B]" /></div>
	{:else}
		{#if filteredCorsi.length === 0}
			<div class="bg-white border-2 border-dashed border-gray-200 rounded-2xl p-20 text-center" in:scale>
				<BookOpen size={48} class="mx-auto text-gray-300 mb-4" />
				<h2 class="text-xl font-bold text-[#1B4B6B] uppercase">Nessun Corso Programmato</h2>
			</div>
		{:else}
			<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-8">
				{#each filteredCorsi as corso (corso.idCorso)}
					<div class="bg-white rounded-2xl shadow-sm border border-gray-100 hover:shadow-xl hover:border-[#1B4B6B]/30 transition-all flex flex-col h-full overflow-hidden group cursor-default" in:scale>
						<div class="p-6 border-b border-gray-50 bg-gray-50/50 flex justify-between items-start">
							<div class="flex items-center gap-3">
								<div class="p-3 bg-white rounded-xl text-[#1B4B6B] shadow-sm group-hover:bg-[#1B4B6B] group-hover:text-white transition-colors">
									<GraduationCap size={24} />
								</div>
								<div>
                            <span class="text-[9px] font-black px-2 py-0.5 rounded uppercase border {corso.stato === StatoCorso.PROGRAMMATO ? 'border-blue-200 text-blue-600 bg-blue-50' : corso.stato === StatoCorso.IN_SVOLGIMENTO ? 'border-yellow-200 text-yellow-600 bg-yellow-50' : 'border-green-200 text-green-600 bg-green-50'}">
                            {corso.stato.replace('_', ' ')}
                            </span>
								</div>
							</div>
							<button
									onclick={() => eliminaCorso(corso.idCorso)}
									class="text-gray-300 hover:text-red-600 transition-colors p-2 rounded-lg hover:bg-red-50"
							>
								<Trash2 size={18} />
							</button>
						</div>

						<div class="p-6 flex-1 flex flex-col gap-4">
							<h3 class="font-extrabold text-[#1B4B6B] text-lg uppercase leading-tight group-hover:text-[#1B4B6B] transition-colors">
								{corso.titolo}
							</h3>

							<div class="space-y-3 mt-2">
								<div class="flex items-start gap-3">
									<Calendar size={16} class="text-gray-400 shrink-0 mt-0.5" />
									<div>
										<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Data e Ora</p>
										<p class="text-xs font-bold text-[#1B4B6B]">{formattaData(corso.dataOrario)}</p>
									</div>
								</div>

								<div class="flex items-start gap-3">
									<MapPin size={16} class="text-gray-400 shrink-0 mt-0.5" />
									<div>
										<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Luogo</p>
										<p class="text-xs font-bold text-[#1B4B6B] uppercase">{corso.luogoFisico}</p>
									</div>
								</div>

								<div class="flex items-start gap-3">
									<Users size={16} class="text-gray-400 shrink-0 mt-0.5" />
									<div>
										<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest">Capacità Aula</p>
										<p class="text-xs font-bold text-[#1B4B6B]">Max {corso.capacitaMassima} iscritti</p>
									</div>
								</div>
							</div>
						</div>

						<div class="bg-[#1B4B6B] p-4 text-white flex justify-between items-center text-[10px] font-bold uppercase tracking-widest group-hover:bg-[#153a54] transition-colors">
							<span>Docente Assegnato</span>
							<span class="truncate max-w-[150px]">{corso.emailDocente}</span>
						</div>
					</div>
				{/each}
			</div>
		{/if}
	{/if}
</div>

{#if showModal}
	<div class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm" in:fade>
		<div class="bg-white w-full max-w-2xl rounded-3xl shadow-2xl overflow-hidden flex flex-col max-h-[90vh]" in:scale>
			<div class="bg-[#1B4B6B] p-8 text-white flex justify-between items-center shrink-0">
				<h2 class="text-2xl font-extrabold uppercase tracking-tight">Programma Nuovo Corso</h2>
				<button onclick={() => showModal = false} class="hover:text-red-400 transition-colors"><X size={28} /></button>
			</div>

			<div class="p-8 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/20">
				<div class="grid grid-cols-1 md:grid-cols-2 gap-6">

					<div class="col-span-2 space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Titolo del Corso *</label>
						<input bind:value={formCorso.titolo} type="text" placeholder="Es: Antincendio Rischio Medio" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl outline-none focus:ring-2 focus:ring-[#1B4B6B] transition-all font-extrabold uppercase text-xs" />
					</div>

					<div class="space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Inizio Sessione *</label>
						<input bind:value={formCorso.dataOrario} type="datetime-local" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl transition-all font-bold text-xs" />
					</div>

					<div class="space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Capacità Massima *</label>
						<input bind:value={formCorso.capacitaMassima} type="number" min="1" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl transition-all font-bold text-xs" />
					</div>

					<div class="col-span-2 space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Sede Aula *</label>
						<input bind:value={formCorso.luogoFisico} type="text" placeholder="Es: Sede NorLan - Aula A" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl transition-all font-bold text-xs uppercase" />
					</div>

					<div class="col-span-2 space-y-2 pt-2 border-t border-gray-100">
						<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Assegnazione Docente *</label>
						<select bind:value={formCorso.idDocente} class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl transition-all font-bold text-xs uppercase cursor-pointer outline-none focus:ring-2 focus:ring-[#1B4B6B]">
							<option value={undefined} disabled>-- Seleziona un docente dal database --</option>
							{#each docenti as docente (docente.idUtente)}
								<option value={docente.idUtente}>{docente.nome} {docente.cognome} ({docente.email})</option>
							{/each}
						</select>
					</div>

				</div>
			</div>

			<div class="p-8 border-t border-gray-100 shrink-0 flex gap-4 bg-white rounded-b-3xl">
				<button onclick={() => showModal = false} class="flex-1 px-6 py-4 border-2 border-gray-100 text-gray-400 font-extrabold rounded-2xl hover:bg-gray-50 transition-all uppercase text-xs">Annulla</button>
				<button
						onclick={salvaNuovoCorso}
						disabled={!isFormValid || isSaving}
						class="flex-1 px-6 py-4 bg-[#1B4B6B] text-white font-extrabold rounded-2xl shadow-xl hover:bg-[#1B4B6B]/90 transition-all uppercase text-xs disabled:opacity-50 disabled:cursor-not-allowed flex justify-center items-center gap-2"
				>
					{#if isSaving} <Loader2 size={16} class="animate-spin" /> Creazione... {:else} Salva e Programma {/if}
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
	.custom-scrollbar::-webkit-scrollbar { width: 5px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>