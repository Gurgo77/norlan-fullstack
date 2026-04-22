<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		BookOpen, Clock, User, CheckCircle2,
		PlayCircle, FileText, Loader2, Search,
		Calendar, FileBadge, Download, Star, MapPin
	} from 'lucide-svelte';

	import { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { AuthService } from '$lib/services/AuthService';

	let isLoading = $state(true);
	let filtroAttivo = $state<'TUTTI' | 'ATTIVI' | 'COMPLETATI'>('TUTTI');
	let searchQuery = $state('');

	let iscrizioni = $state<IscrizioneCorso[]>([]);
	let currentUser = AuthService.getSession();

	onMount(() => {
		setTimeout(() => {
			const mockData = [
				{
					idUtente: currentUser?.idUtente || 501,
					idCorso: 1,
					emailUtente: currentUser?.email || 'mario.rossi@norlan.it',
					titoloCorso: 'SICUREZZA SUL LAVORO - RISCHIO ALTO',
					dataOrarioCorso: '2026-05-15T14:30:00',
					presenzaConfermata: true,
					pathAttestato: ''
				},
				{
					idUtente: currentUser?.idUtente || 501,
					idCorso: 2,
					emailUtente: currentUser?.email || 'mario.rossi@norlan.it',
					titoloCorso: 'ANTINCENDIO LIVELLO 2',
					dataOrarioCorso: '2026-02-10T09:00:00',
					presenzaConfermata: true,
					pathAttestato: '/uploads/attestati/mario_rossi_antincendio.pdf'
				}
			];

			iscrizioni = mockData.map(d => new IscrizioneCorso(d));
			isLoading = false;
		}, 600);
	});

	const iscrizioniFiltrate = $derived(
		iscrizioni.filter(i => {
			const completato = i.pathAttestato !== '';
			const matchFiltro =
				filtroAttivo === 'TUTTI' ||
				(filtroAttivo === 'COMPLETATI' && completato) ||
				(filtroAttivo === 'ATTIVI' && !completato);

			const matchSearch = i.titoloCorso.toLowerCase().includes(searchQuery.toLowerCase());
			return matchFiltro && matchSearch;
		})
	);

	function formatData(isoString: string) {
		return new Date(isoString).toLocaleDateString('it-IT', {
			day: '2-digit', month: 'long', hour: '2-digit', minute: '2-digit'
		}).toUpperCase();
	}
</script>

<div in:fade class="max-w-7xl mx-auto space-y-8 pb-10">

	<div class="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">I Miei Corsi</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Gestione iscrizioni e download attestati</p>
		</div>

		<div class="flex bg-gray-100 p-1.5 rounded-2xl gap-1">
			{#each ['TUTTI', 'ATTIVI', 'COMPLETATI'] as opzione (opzione)}
				<button
					onclick={() => filtroAttivo = opzione as typeof filtroAttivo}
					class="px-6 py-2.5 rounded-xl text-[10px] font-black uppercase tracking-widest transition-all
					{filtroAttivo === opzione ? 'bg-[#1B4B6B] text-white shadow-md' : 'text-gray-400 hover:text-[#1B4B6B]'}"
				>
					{opzione}
				</button>
			{/each}
		</div>
	</div>

	<div class="relative group max-w-md">
		<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
		<input
			bind:value={searchQuery}
			type="text"
			placeholder="CERCA CORSO..."
			class="w-full pl-12 pr-6 py-4 bg-white border border-gray-100 rounded-[1.5rem] text-xs font-bold uppercase outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 shadow-sm transition-all"
		/>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Caricamento registro...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
			{#each iscrizioniFiltrate as iscrizione (iscrizione.idCorso)}
				{@const isCompletato = iscrizione.pathAttestato !== ''}
				<div
					in:scale={{duration: 300}}
					class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm flex flex-col md:flex-row overflow-hidden hover:shadow-xl transition-all group"
				>
					<div class="w-full md:w-32 flex flex-col items-center justify-center p-6 border-b md:border-b-0 md:border-r border-gray-50 {isCompletato ? 'bg-emerald-50/30' : 'bg-gray-50/30'}">
						<div class="p-4 rounded-2xl mb-2 {isCompletato ? 'bg-emerald-500 text-white' : 'bg-[#1B4B6B] text-white'} shadow-lg">
							{#if isCompletato} <FileBadge size={28} /> {:else} <BookOpen size={28} /> {/if}
						</div>
						<span class="text-[8px] font-black uppercase tracking-widest text-center {isCompletato ? 'text-emerald-600' : 'text-[#1B4B6B]'}">
							{isCompletato ? 'COMPLETATO' : 'IN CORSO'}
						</span>
					</div>

					<div class="flex-1 p-8 flex flex-col justify-between">
						<div>
							<h3 class="text-xl font-black text-[#1B4B6B] uppercase leading-tight mb-4">{iscrizione.titoloCorso}</h3>
							<div class="flex flex-wrap gap-4">
								<div class="flex items-center gap-2 text-[10px] font-bold text-gray-400 uppercase">
									<Calendar size={14} class="text-[#1B4B6B]" />
									{formatData(iscrizione.dataOrarioCorso)}
								</div>
								<div class="flex items-center gap-2 text-[10px] font-bold text-gray-400 uppercase">
									<CheckCircle2 size={14} class={iscrizione.presenzaConfermata ? 'text-emerald-500' : 'text-gray-300'} />
									{iscrizione.presenzaConfermata ? 'Presenza OK' : 'Da confermare'}
								</div>
							</div>
						</div>

						<div class="mt-8 flex gap-3">
							{#if isCompletato}
								<button class="flex-1 bg-emerald-500 text-white py-4 rounded-2xl text-[10px] font-black uppercase flex items-center justify-center gap-2 shadow-lg shadow-emerald-900/10 hover:bg-emerald-600 transition-all">
									<Download size={18} /> Scarica
								</button>
								<button class="px-6 bg-gray-50 text-[#1B4B6B] py-4 rounded-2xl hover:bg-gray-100 transition-all text-gray-400">
									<Star size={18} />
								</button>
							{:else}
								<button class="flex-1 bg-[#1B4B6B] text-white py-4 rounded-2xl text-[10px] font-black uppercase flex items-center justify-center gap-2 shadow-lg shadow-blue-900/10 hover:bg-[#153a54] transition-all">
									<PlayCircle size={18} /> Materiali
								</button>
								<button class="px-6 bg-gray-50 text-[#1B4B6B] py-4 rounded-2xl hover:bg-gray-100 transition-all text-gray-400">
									<MapPin size={18} />
								</button>
							{/if}
						</div>
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>

<style>
    :global(body) { background-color: #F9FAFB; }
</style>