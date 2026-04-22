<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Users, Search, Filter, Mail, FileBadge, Upload, BookOpen, Loader2
	} from 'lucide-svelte';
	import { IscrizioneCorso } from '$lib/models/IscrizioneCorso';

	let isLoading = $state(true);
	let studenti = $state<IscrizioneCorso[]>([]);
	let queryRicerca = $state('');
	let filtroCorso = $state('');

	onMount(() => {
		setTimeout(() => {
			const mockData = [
				{
					idUtente: 101,
					idCorso: 1,
					emailUtente: 'mario.rossi@logistica.it',
					titoloCorso: 'SICUREZZA SUL LAVORO - RISCHIO ALTO',
					dataOrarioCorso: '2026-05-15T09:00:00',
					presenzaConfermata: true,
					pathAttestato: '/uploads/attestati/101_1.pdf'
				},
				{
					idUtente: 102,
					idCorso: 1,
					emailUtente: 'anna.verdi@costruzioni.it',
					titoloCorso: 'SICUREZZA SUL LAVORO - RISCHIO ALTO',
					dataOrarioCorso: '2026-05-15T09:00:00',
					presenzaConfermata: false,
					pathAttestato: '' // <--- Corretto qui: passiamo stringa vuota invece di null
				},
				{
					idUtente: 103,
					idCorso: 2,
					emailUtente: 'luca.bianchi@tech.com',
					titoloCorso: 'AGGIORNAMENTO ANTINCENDIO',
					dataOrarioCorso: '2026-05-22T14:30:00',
					presenzaConfermata: false,
					pathAttestato: '' // <--- Corretto qui: passiamo stringa vuota invece di null
				}
			];

			studenti = mockData.map(item => new IscrizioneCorso(item));
			isLoading = false;
		}, 800);
	});

	const studentiFiltrati = $derived(
		studenti.filter(s => {
			const matchTesto = s.emailUtente.toLowerCase().includes(queryRicerca.toLowerCase());
			const matchCorso = filtroCorso === '' || s.idCorso.toString() === filtroCorso;
			return matchTesto && matchCorso;
		})
	);

	const corsiUnici = $derived(
		Array.from(new Set(studenti.map(s => s.idCorso))).map(id => {
			const studente = studenti.find(s => s.idCorso === id);
			return { id, titolo: studente?.titoloCorso };
		})
	);

	function getIniziale(email: string) {
		return email ? email.charAt(0).toUpperCase() : 'S';
	}
</script>

<div in:fade class="space-y-8 max-w-7xl mx-auto">
	<div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Iscritti ai Corsi</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Gestione presenze, comunicazioni e rilascio attestati</p>
		</div>

		<div class="flex gap-4">
			<div class="bg-white px-6 py-4 rounded-3xl shadow-sm border border-gray-100 flex items-center gap-4">
				<div class="p-3 bg-blue-50 rounded-2xl text-[#1B4B6B]">
					<Users size={24} />
				</div>
				<div>
					<p class="text-[10px] font-black text-gray-300 uppercase tracking-widest">Totale Iscritti</p>
					<p class="text-lg font-black text-[#1B4B6B] uppercase">{studenti.length} STUDENTI</p>
				</div>
			</div>
		</div>
	</div>

	<div class="bg-white p-4 rounded-3xl shadow-sm border border-gray-100 flex flex-col lg:flex-row gap-4">
		<div class="relative flex-1 group">
			<Search class="absolute left-5 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
			<input
				bind:value={queryRicerca}
				type="text"
				placeholder="CERCA STUDENTE PER EMAIL..."
				class="w-full bg-gray-50 border-none rounded-2xl py-4 pl-14 pr-6 text-xs font-bold text-[#1B4B6B] placeholder:text-gray-400 focus:ring-4 focus:ring-[#1B4B6B]/10 transition-all uppercase outline-none"
			/>
		</div>
		<div class="relative min-w-[300px]">
			<Filter class="absolute left-5 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
			<select
				bind:value={filtroCorso}
				class="w-full bg-gray-50 border-none rounded-2xl py-4 pl-14 pr-10 text-xs font-bold text-[#1B4B6B] focus:ring-4 focus:ring-[#1B4B6B]/10 transition-all uppercase outline-none appearance-none cursor-pointer"
			>
				<option value="">TUTTI I CORSI</option>
				{#each corsiUnici as corso (corso.id)}
					<option value={corso.id.toString()}>{corso.titolo}</option>
				{/each}
			</select>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-[0.2em]">Caricamento iscritti...</span>
		</div>
	{:else if studentiFiltrati.length === 0}
		<div class="py-32 bg-white rounded-3xl border border-gray-100 border-dashed flex flex-col items-center justify-center text-center shadow-sm">
			<Users size={48} class="text-gray-200 mb-4" />
			<h3 class="font-black text-[#1B4B6B] uppercase text-lg">Nessuno studente trovato</h3>
			<p class="text-[10px] font-bold text-gray-400 uppercase mt-2">Modifica i parametri di ricerca o il filtro corso</p>
		</div>
	{:else}
		<div class="space-y-4">
			{#each studentiFiltrati as studente (studente.idUtente + '-' + studente.idCorso)}
				<div
					in:scale={{duration: 300}}
					class="bg-white rounded-[2rem] border border-gray-100 shadow-sm hover:shadow-xl hover:border-[#1B4B6B]/30 transition-all duration-300 p-6 flex flex-col xl:flex-row items-start xl:items-center justify-between gap-6 group"
				>
					<div class="flex items-center gap-5 w-full xl:w-auto">
						<div class="w-14 h-14 rounded-2xl bg-[#1B4B6B] text-white flex items-center justify-center font-black text-xl shadow-md shrink-0">
							{getIniziale(studente.emailUtente)}
						</div>
						<div class="flex-1 min-w-0">
							<h4 class="font-extrabold text-[#1B4B6B] uppercase text-lg truncate">{studente.emailUtente}</h4>
							<div class="flex items-center gap-3 mt-1 text-[10px] font-black uppercase tracking-widest text-gray-400">
								<span class="flex items-center gap-1.5"><BookOpen size={12} /> {studente.titoloCorso}</span>
							</div>
						</div>
					</div>

					<div class="flex flex-wrap xl:flex-nowrap items-center gap-3 w-full xl:w-auto">
						<button class="flex-1 xl:flex-none flex items-center justify-center gap-2 bg-gray-50 border border-gray-200 text-[#1B4B6B] px-5 py-3.5 rounded-xl text-[10px] font-black uppercase hover:bg-blue-50 hover:text-blue-600 hover:border-blue-100 transition-all">
							<Mail size={16} />
							Contatta
						</button>

						<div class="h-10 w-px bg-gray-100 hidden xl:block"></div>

						<label class="flex-1 xl:flex-none relative inline-flex items-center cursor-pointer bg-gray-50 px-4 py-3 rounded-xl border border-gray-100">
							<input type="checkbox" bind:checked={studente.presenzaConfermata} class="sr-only peer">
							<div class="w-12 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[14px] after:left-[18px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-green-500 shadow-inner"></div>
							<span class="ml-3 text-[10px] font-black uppercase {studente.presenzaConfermata ? 'text-green-600' : 'text-gray-400'}">
								{studente.presenzaConfermata ? 'PRESENTE' : 'ASSENTE'}
							</span>
						</label>

						{#if studente.presenzaConfermata}
							<div class="flex-1 xl:flex-none">
								{#if studente.pathAttestato !== ''}
									<button class="w-full xl:w-auto flex items-center justify-center gap-2 bg-emerald-50 text-emerald-600 border border-emerald-200 px-5 py-3.5 rounded-xl text-[10px] font-black uppercase hover:bg-emerald-100 transition-all">
										<FileBadge size={16} />
										Attestato Pronto
									</button>
								{:else}
									<button class="w-full xl:w-auto flex items-center justify-center gap-2 bg-[#1B4B6B] text-white border border-[#1B4B6B] px-5 py-3.5 rounded-xl text-[10px] font-black uppercase hover:bg-[#153a54] transition-all shadow-md">
										<Upload size={16} />
										Carica Attestato
									</button>
								{/if}
							</div>
						{:else}
							<div class="flex-1 xl:flex-none flex items-center justify-center gap-2 bg-gray-50 text-gray-300 border border-gray-100 px-5 py-3.5 rounded-xl text-[10px] font-black uppercase cursor-not-allowed">
								<FileBadge size={16} />
								Richiede Presenza
							</div>
						{/if}
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>