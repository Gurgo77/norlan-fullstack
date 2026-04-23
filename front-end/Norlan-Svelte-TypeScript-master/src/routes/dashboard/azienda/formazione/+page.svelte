<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Search,
		Filter,
		User,
		ShieldCheck,
		AlertTriangle,
		ArrowRight,
		Bookmark,
		Loader2,
		GraduationCap // Importazione assicurata per risolvere no-undef
	} from 'lucide-svelte';

	// Import Servizi e Modelli
	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';

	// --- INTERFACCE TIPIZZATE ---
	interface CorsoStato {
		nome: string;
		data: string;
		stato: 'OK' | 'IN_ATTESA' | 'CRITICO';
	}

	interface DipendenteFormazione {
		id: number;
		nomeCompleto: string;
		ruolo: string;
		corsi: CorsoStato[];
	}

	// --- STATO REATTIVO (Svelte 5) ---
	let isLoading = $state(true);
	let searchQuery = $state('');
	let dipendenti = $state<DipendenteFormazione[]>([]);

	// --- CARICAMENTO DATI ---
	onMount(async () => {
		const session = AuthService.getSession(); //
		if (!session) return;

		try {
			// 1. Recupero i lavoratori associati all'azienda
			const lavoratoriRaw = await LavoratoreService.getByAzienda(session.idUtente);

			// 2. Costruzione della lista con lo stato formativo reale
			const promises = lavoratoriRaw.map(async (l) => {
				const iscrizioni = await FormazioneService.getIscrizioniUtente(l.idUtente);

				return {
					id: l.idUtente,
					nomeCompleto: `${l.nome} ${l.cognome}`.toUpperCase(),
					ruolo: l.ruolo.replace('_', ' '),
					corsi: iscrizioni.map((i) => ({
						nome: i.titoloCorso.toUpperCase(),
						data: formattaData(i.dataOrarioCorso),
						// Cast esplicito per risolvere errore assignable (Immagine 17cde2.png)
						stato: (i.presenzaConfermata ? 'OK' : 'IN_ATTESA') as 'OK' | 'IN_ATTESA'
					}))
				};
			});

			dipendenti = await Promise.all(promises);
		} catch (error) {
			console.error('Errore nel recupero dei registri formazione:', error);
		} finally {
			isLoading = false;
		}
	});

	// --- LOGICA REATTIVA ---
	const filteredDipendenti = $derived(
			dipendenti.filter((d) => d.nomeCompleto.toLowerCase().includes(searchQuery.toLowerCase()))
	);

	const countDaAggiornare = $derived(
			dipendenti.filter((d) => d.corsi.some((c) => c.stato !== 'OK')).length
	);

	function formattaData(dateStr: string) {
		return new Date(dateStr).toLocaleDateString('it-IT', {
			day: '2-digit',
			month: '2-digit',
			year: 'numeric'
		});
	}
</script>

<div in:fade>
	<div class="mb-10 flex items-start justify-between">
		<div>
			<h1 class="text-4xl font-extrabold uppercase tracking-tighter text-[#1B4B6B]">
				Formazione Dipendenti
			</h1>
			<p class="text-xs font-bold uppercase tracking-tighter text-gray-500">
				Monitoraggio attestati e scadenze corsi NorLan.
			</p>
		</div>

		<div class="flex items-center gap-4 rounded-2xl border border-red-50 bg-white p-4 shadow-sm">
			<div class="rounded-lg bg-red-50 p-2 text-red-500">
				<AlertTriangle size={20} />
			</div>
			<div>
				<p class="text-[9px] font-bold uppercase text-gray-400">Da Completare</p>
				<p class="text-xs font-black uppercase text-red-600">{countDaAggiornare} Dipendenti</p>
			</div>
		</div>
	</div>

	<div class="mb-10 flex gap-4">
		<div class="group relative flex-1">
			<Search
					class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 transition-colors group-focus-within:text-[#1B4B6B]"
					size={18}
			/>
			<input
					bind:value={searchQuery}
					type="text"
					placeholder="Cerca dipendente per nome..."
					class="w-full rounded-2xl border border-gray-100 bg-white py-4 pl-12 pr-4 text-xs font-bold uppercase outline-none transition-all focus:ring-4 focus:ring-[#1B4B6B]/5"
			/>
		</div>
		<button
				class="rounded-2xl border border-gray-100 bg-white p-4 text-gray-400 shadow-sm transition-all hover:text-[#1B4B6B]"
		>
			<Filter size={20} />
		</button>
	</div>

	{#if isLoading}
		<div class="flex flex-col items-center justify-center gap-4 py-20">
			<Loader2 size={40} class="animate-spin text-[#1B4B6B]" />
			<p class="text-[10px] font-black uppercase tracking-widest text-gray-300">
				Sincronizzazione registri...
			</p>
		</div>
	{:else}
		<div class="space-y-6">
			{#each filteredDipendenti as dip (dip.id)}
				<div
						class="group flex cursor-default items-center gap-8 rounded-[32px] border border-gray-100 bg-white p-6 shadow-sm transition-all duration-300 hover:border-[#1B4B6B]/20 hover:shadow-xl"
						in:scale
				>
					<div
							class="flex size-16 flex-none shrink-0 items-center justify-center rounded-2xl bg-[#1B4B6B] text-white shadow-lg shadow-[#1B4B6B]/20"
					>
						<User size={28} />
					</div>

					<div class="w-64 shrink-0">
						<h3 class="text-lg font-black uppercase leading-tight text-[#1B4B6B]">
							{dip.nomeCompleto}
						</h3>
						<p class="text-[10px] font-bold uppercase tracking-tight text-gray-400">{dip.ruolo}</p>
					</div>

					<div class="flex flex-1 flex-wrap gap-3">
						{#each dip.corsi as corso, index (corso.nome + index)}
							<div
									class="flex items-center gap-3 rounded-xl border px-4 py-2 {corso.stato === 'OK'
									? 'border-green-100 bg-green-50 text-green-600'
									: 'border-yellow-100 bg-yellow-50 text-yellow-600'}"
							>
								<div>
									<p class="text-[9px] font-black uppercase leading-none tracking-tighter">
										{corso.nome}
									</p>
									<p class="mt-1 text-[10px] font-bold opacity-80">{corso.data}</p>
								</div>
								{#if corso.stato === 'OK'}
									<ShieldCheck size={14} />
								{:else}
									<AlertTriangle size={14} />
								{/if}
							</div>
						{/each}
						{#if dip.corsi.length === 0}
							<p class="text-[10px] font-bold uppercase italic text-gray-300">
								Nessun corso registrato
							</p>
						{/if}
					</div>

					<div class="flex items-center gap-4">
						<button class="text-gray-300 transition-colors hover:text-[#1B4B6B]">
							<Bookmark size={20} />
						</button>

						<a
								href="/dashboard/azienda/dipendenti"
								class="flex items-center gap-3 rounded-2xl border-2 border-[#1B4B6B] bg-white px-6 py-3 text-[11px] font-black uppercase tracking-widest text-[#1B4B6B] shadow-sm transition-all hover:bg-[#1B4B6B] hover:text-white"
						>
							Profilo
							<ArrowRight size={16} />
						</a>
					</div>
				</div>
			{/each}
		</div>
	{/if}

	{#if !isLoading && filteredDipendenti.length === 0}
		<div class="rounded-[40px] border-2 border-dashed border-gray-100 bg-white p-20 text-center">
			<GraduationCap size={48} class="mx-auto mb-4 text-gray-200" />
			<h3 class="text-xl font-black uppercase text-[#1B4B6B]">Nessun dipendente trovato</h3>
			<p class="mt-2 text-[10px] font-bold uppercase tracking-[0.2em] text-gray-400">
				Verifica i criteri di ricerca nel database NorLan.
			</p>
		</div>
	{/if}
</div>

<style>
	:global(body) {
		background-color: #f9fafb;
	}
</style>