<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		GraduationCap, Plus, X, Trash2, Search,
		Calendar, MapPin, Users, BookOpen, AlertCircle
	} from 'lucide-svelte';
	import { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { StatoCorso } from '$lib/models/Enums';

	// --- FIX TYPE ANY ---
	interface CorsoRaw {
		idCorso: number;
		titolo: string;
		dataOrario: string;
		luogoFisico: string;
		capacitaMassima: number;
		idDocente: number;
		emailDocente: string;
		stato: StatoCorso;
	}

	let corsi = $state<CorsoFormazione[]>([]);
	let isLoading = $state(true);
	let searchQuery = $state('');
	let showModal = $state(false);

	let formCorso = $state({
		titolo: '',
		dataOrario: '',
		luogoFisico: '',
		capacitaMassima: 20,
		idDocente: 1,
		emailDocente: 'docente@norlan.it',
		stato: StatoCorso.PROGRAMMATO
	});

	onMount(() => {
		const datiSalvati = localStorage.getItem('norlan_corsi_test');
		if (datiSalvati) {
			const parsed: CorsoRaw[] = JSON.parse(datiSalvati);
			corsi = parsed.map((c) => new CorsoFormazione(c));
		}
		isLoading = false;
	});

	function sincronizzaLocale() {
		localStorage.setItem('norlan_corsi_test', JSON.stringify(corsi));
	}

	function salvaNuovoCorso() {
		if (!formCorso.titolo || !formCorso.dataOrario || !formCorso.luogoFisico) return;

		const nuovoCorso = new CorsoFormazione({
			idCorso: Math.floor(Date.now()),
			titolo: formCorso.titolo,
			dataOrario: formCorso.dataOrario,
			luogoFisico: formCorso.luogoFisico,
			capacitaMassima: formCorso.capacitaMassima,
			idDocente: formCorso.idDocente,
			emailDocente: formCorso.emailDocente,
			stato: formCorso.stato
		});

		corsi = [...corsi, nuovoCorso];
		sincronizzaLocale();

		formCorso = {
			titolo: '',
			dataOrario: '',
			luogoFisico: '',
			capacitaMassima: 20,
			idDocente: 1,
			emailDocente: 'docente@norlan.it',
			stato: StatoCorso.PROGRAMMATO
		};
		showModal = false;
	}

	function eliminaCorso(idCorso: number) {
		corsi = corsi.filter(c => c.idCorso !== idCorso);
		sincronizzaLocale();
	}

	const filteredCorsi = $derived(
		corsi.filter(c => c.titolo.toLowerCase().includes(searchQuery.toLowerCase()))
	);

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

	{#if !isLoading}
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
				<button onclick={() => showModal = false}><X size={28} /></button>
			</div>

			<div class="p-8 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/20">
				<div class="grid grid-cols-1 md:grid-cols-2 gap-6">

					<div class="col-span-2 space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Titolo del Corso</label>
						<input bind:value={formCorso.titolo} type="text" placeholder="Es: Antincendio Rischio Medio" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl outline-none focus:ring-2 focus:ring-[#1B4B6B] transition-all font-extrabold uppercase text-xs" />
					</div>

					<div class="space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Inizio Sessione</label>
						<input bind:value={formCorso.dataOrario} type="datetime-local" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl transition-all font-bold text-xs" />
					</div>

					<div class="space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Capacità</label>
						<input bind:value={formCorso.capacitaMassima} type="number" min="1" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl transition-all font-bold text-xs" />
					</div>

					<div class="col-span-2 space-y-2">
						<label class="text-[10px] font-bold text-gray-400 uppercase ml-1">Sede Aula</label>
						<input bind:value={formCorso.luogoFisico} type="text" placeholder="Es: Sede NorLan - Aula A" class="w-full px-5 py-3 bg-white border border-gray-200 rounded-2xl transition-all font-bold text-xs uppercase" />
					</div>

					<div class="col-span-2 p-4 bg-blue-50 border border-blue-100 rounded-2xl flex items-start gap-3 mt-2 shadow-sm">
						<AlertCircle size={18} class="text-blue-600 shrink-0 mt-0.5" />
						<div>
							<p class="text-xs font-bold text-blue-800 uppercase">Assegnazione Docente</p>
							<p class="text-[10px] font-medium text-blue-600 mt-1">L'assegnazione automatica dei docenti certificati sarà attiva dopo il collegamento al server.</p>
						</div>
					</div>

				</div>
			</div>

			<div class="p-8 border-t border-gray-100 shrink-0 flex gap-4 bg-white rounded-b-3xl">
				<button onclick={() => showModal = false} class="flex-1 px-6 py-4 border-2 border-gray-100 text-gray-400 font-extrabold rounded-2xl hover:bg-gray-50 transition-all uppercase text-xs">Annulla</button>
				<button onclick={salvaNuovoCorso} class="flex-1 px-6 py-4 bg-[#1B4B6B] text-white font-extrabold rounded-2xl shadow-xl hover:bg-[#1B4B6B]/90 transition-all uppercase text-xs">
					Salva e Programma
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
    .custom-scrollbar::-webkit-scrollbar { width: 5px; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>