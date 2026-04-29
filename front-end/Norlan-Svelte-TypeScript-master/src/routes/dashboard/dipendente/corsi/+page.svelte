<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		BookOpen,
		PlayCircle,
		Loader2,
		Search,
		Calendar,
		MapPin
	} from 'lucide-svelte';

	// IMPORT SERVIZI E MODELLI UFFICIALI
	import type { IscrizioneCorso } from '$lib/models/IscrizioneCorso';
	import { AuthService } from '$lib/services/AuthService';
	import { FormazioneService } from '$lib/services/FormazioneService';

	// --- STATO REATTIVO (Svelte 5) ---
	let isLoading = $state(true);
	let searchQuery = $state('');
	let iscrizioni = $state<IscrizioneCorso[]>([]);

	// --- CARICAMENTO DATI ---
	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			// Recupero iscrizioni reali del dipendente loggato
			iscrizioni = await FormazioneService.getIscrizioniUtente(session.idUtente);
		} catch (error) {
			console.error('Errore nel recupero dei corsi:', error);
		} finally {
			isLoading = false;
		}
	});

	// --- LOGICA REATTIVA (REGOLA 6) ---
	const iscrizioniAttive = $derived(
			iscrizioni.filter((i) => {
				// Il corso scompare non appena passa a CONCLUSO (o stati successivi).
				// Il dipendente non deve più farci nulla: scaricherà l'attestato dall'altra pagina.
				const isOperativo = i.statoCorso === 'PROGRAMMATO' || i.statoCorso === 'IN_SVOLGIMENTO';
				const matchSearch = (i.titoloCorso || '').toLowerCase().includes(searchQuery.toLowerCase());

				return isOperativo && matchSearch;
			})
	);

	function formatData(isoString: string) {
		if (!isoString) return 'Data non definita';
		return new Date(isoString)
				.toLocaleDateString('it-IT', {
					day: '2-digit',
					month: 'long',
					hour: '2-digit',
					minute: '2-digit'
				})
				.toUpperCase();
	}
</script>

<div in:fade class="mx-auto max-w-7xl space-y-8 pb-10">
	<div class="flex flex-col items-start justify-between gap-6 md:flex-row md:items-end">
		<div>
			<h1 class="text-4xl font-black uppercase tracking-tighter text-[#1B4B6B]">I Miei Corsi</h1>
			<p class="mt-1 text-[10px] font-bold uppercase tracking-widest text-gray-400">
				Prossime lezioni e aule virtuali in programma
			</p>
		</div>
	</div>

	<div class="group relative max-w-md">
		<Search
				class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 transition-colors group-focus-within:text-[#1B4B6B]"
				size={20}
		/>
		<input
				bind:value={searchQuery}
				type="text"
				placeholder="CERCA CORSO IN PROGRAMMA..."
				class="w-full rounded-[1.5rem] border border-gray-100 bg-white py-4 pl-12 pr-6 text-xs font-bold uppercase shadow-sm outline-none transition-all focus:ring-4 focus:ring-[#1B4B6B]/5"
		/>
	</div>

	{#if isLoading}
		<div class="flex flex-col items-center justify-center gap-4 py-32">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black uppercase tracking-widest text-gray-400">
             Sincronizzazione registro corsi...
          </span>
		</div>
	{:else}
		<div class="grid grid-cols-1 gap-8 lg:grid-cols-2">
			{#each iscrizioniAttive as iscrizione (iscrizione.idCorso)}
				<div
						in:scale={{ duration: 300 }}
						class="group flex flex-col overflow-hidden rounded-[2.5rem] border border-gray-100 bg-white shadow-sm transition-all hover:shadow-xl md:flex-row"
				>
					<div class="flex w-full flex-col items-center justify-center border-b border-gray-50 bg-gray-50/30 p-6 md:w-32 md:border-b-0 md:border-r">
						<div class="mb-2 rounded-2xl bg-[#1B4B6B] p-4 text-white shadow-lg">
							<BookOpen size={28} />
						</div>
						<span class="text-center text-[8px] font-black uppercase tracking-widest text-[#1B4B6B]">
                      {iscrizione.statoCorso === 'IN_SVOLGIMENTO' ? 'IN CORSO' : 'IN ARRIVO'}
                   </span>
					</div>

					<div class="flex flex-1 flex-col justify-between p-8">
						<div>
							<h3 class="mb-4 text-xl font-black uppercase leading-tight text-[#1B4B6B]">
								{iscrizione.titoloCorso}
							</h3>
							<div class="flex flex-wrap gap-4">
								<div class="flex items-center gap-2 text-[10px] font-bold uppercase text-gray-400">
									<Calendar size={14} class="text-[#1B4B6B]" />
									{formatData(iscrizione.dataOrarioCorso)}
								</div>
							</div>
						</div>

						<div class="mt-8 flex gap-3">
							<button class="flex flex-1 items-center justify-center gap-2 rounded-2xl bg-[#1B4B6B] py-4 text-[10px] font-black uppercase text-white shadow-lg shadow-blue-900/10 transition-all hover:bg-[#153a54]">
								<PlayCircle size={18} /> Materiali
							</button>
							<button class="rounded-2xl bg-gray-50 px-6 py-4 text-gray-400 transition-all hover:bg-gray-100">
								<MapPin size={18} />
							</button>
						</div>
					</div>
				</div>
			{/each}

			{#if iscrizioniAttive.length === 0 && !isLoading}
				<div class="col-span-1 rounded-[2.5rem] border border-dashed border-gray-200 bg-gray-50 py-20 text-center lg:col-span-2">
					<BookOpen size={48} class="mx-auto mb-4 text-gray-300" />
					<h3 class="text-xl font-black uppercase italic text-[#1B4B6B]">Nessun corso in programma</h3>
					<p class="mt-2 text-[10px] font-bold uppercase text-gray-400">
						Hai completato tutte le tue attività formative o non ti è stato assegnato nulla.
					</p>
				</div>
			{/if}
		</div>
	{/if}
</div>

<style>
	:global(body) {
		background-color: #f9fafb;
	}
</style>