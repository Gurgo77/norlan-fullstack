<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		MessageSquare, Search, Loader2, User,
		Send, Trash2, ShieldAlert, Bell, X
	} from 'lucide-svelte';

	// IMPORT DAI MODELLI REALI
	import { Messaggio } from '$lib/models/Messaggio';
	import { Dipendente } from '$lib/models/Dipendente';
	import { AuthService } from '$lib/services/AuthService';

	// STATO CON RUNE SVELTE 5
	let isLoading = $state(true);
	let searchQuery = $state('');
	let messaggioSelezionato = $state<Messaggio | null>(null);

	let utente = $state<Dipendente | null>(null);
	let messaggi = $state<Messaggio[]>([]);

	onMount(() => {
		// Carichiamo l'utente reale dal tuo servizio
		utente = AuthService.getSession();

		setTimeout(() => {
			// Mock dati basato sulla tua classe Messaggio
			// Uso "as any" solo per il mock per bypassare discrepanze temporanee nel costruttore
			const datiMock = [
				{
					id: 1,
					idMittente: 10,
					mittente: 'Segreteria NorLan',
					oggetto: 'SCADENZA REVISIONE DPI',
					testo: `Gentile ${utente?.nome || 'Dipendente'}, ti ricordiamo la scadenza dei tuoi DPI.`,
					dataInvio: '2026-04-20T09:00:00',
					letto: false,
					// Se il tuo modello non ha priorità, questo verrà ignorato o darà errore in TS
					// ma lo gestiamo nel template con un controllo sicuro
					priorita: 'ALTA'
				},
				{
					id: 2,
					idMittente: 22,
					mittente: 'Ing. Bianchi',
					oggetto: 'MATERIALE DIDATTICO CORSO',
					testo: 'Le slide del corso sono state caricate nella tua area documenti.',
					dataInvio: '2026-04-18T14:30:00',
					letto: true,
					priorita: 'NORMALE'
				}
			];

			// Creiamo istanze reali della tua classe Messaggio
			messaggi = datiMock.map(d => new Messaggio(d as any));
			isLoading = false;
		}, 600);
	});

	// LOGICA DI FILTRO (Runa $derived)
	const messaggiFiltrati = $derived(
		messaggi.filter(m =>
			m.oggetto.toLowerCase().includes(searchQuery.toLowerCase()) ||
			m.mittente.toLowerCase().includes(searchQuery.toLowerCase())
		)
	);

	// Funzione per selezionare il messaggio ed evitare errori "Unexpected token"
	function seleziona(m: Messaggio) {
		messaggioSelezionato = m;
		m.letto = true;
	}
</script>

<div in:fade class="max-w-7xl mx-auto space-y-8 pb-10">

	<header class="flex flex-col md:flex-row justify-between items-start md:items-end gap-6">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Comunicazioni</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">
				Utente: <span class="text-[#1B4B6B]">{utente?.nome} {utente?.cognome}</span>
			</p>
		</div>
	</header>

	<div class="grid grid-cols-1 xl:grid-cols-12 gap-8 h-[650px]">

		<div class="xl:col-span-5 flex flex-col gap-4 overflow-hidden">
			<div class="relative group shrink-0">
				<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-300" size={18} />
				<input
					bind:value={searchQuery}
					type="text"
					placeholder="CERCA MESSAGGIO..."
					class="w-full pl-12 pr-6 py-4 bg-white border border-gray-100 rounded-2xl text-xs font-bold uppercase outline-none shadow-sm"
				/>
			</div>

			<div class="flex-1 overflow-y-auto pr-2 space-y-3 custom-scrollbar-data">
				{#if isLoading}
					<div class="py-10 flex justify-center"><Loader2 class="animate-spin text-[#1B4B6B]" /></div>
				{:else}
					{#each messaggiFiltrati as m (m.id)}
						<button
							onclick={() => seleziona(m)}
							class="w-full text-left p-5 rounded-[2rem] border transition-all flex gap-4
							{messaggioSelezionato?.id === m.id ? 'bg-[#1B4B6B] border-[#1B4B6B] shadow-xl text-white' : 'bg-white border-gray-100 hover:border-blue-200'}
							{!m.letto ? 'border-l-4 border-l-blue-500' : ''}"
						>
							<div class="w-12 h-12 rounded-xl flex items-center justify-center shrink-0
								{messaggioSelezionato?.id === m.id ? 'bg-white/10' : 'bg-gray-50 text-[#1B4B6B]'}">

								{#if 'priorita' in m && m.priorita === 'ALTA'}
									<ShieldAlert size={20} class={messaggioSelezionato?.id === m.id ? 'text-amber-400' : 'text-red-500'} />
								{:else}
									<Bell size={20} />
								{/if}
							</div>

							<div class="flex-1 min-w-0">
								<div class="flex justify-between mb-1">
									<span class="text-[9px] font-black uppercase {messaggioSelezionato?.id === m.id ? 'text-white/60' : 'text-gray-400'}">{m.mittente}</span>
								</div>
								<h4 class="text-sm font-black uppercase truncate">{m.oggetto}</h4>
							</div>
						</button>
					{/each}
				{/if}
			</div>
		</div>

		<div class="xl:col-span-7 h-full">
			{#if messaggioSelezionato}
				<div in:scale={{duration: 200, start: 0.98}} class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm h-full flex flex-col overflow-hidden">
					<div class="p-8 border-b border-gray-50 flex justify-between items-center bg-gray-50/30">
						<div class="flex items-center gap-4">
							<div class="w-12 h-12 bg-[#1B4B6B] rounded-xl flex items-center justify-center text-white font-bold">
								<User size={24} />
							</div>
							<div>
								<h3 class="text-lg font-black text-[#1B4B6B] uppercase leading-none">{messaggioSelezionato.mittente}</h3>
							</div>
						</div>
						<button onclick={() => messaggioSelezionato = null} class="p-3 bg-white border border-gray-100 text-gray-400 rounded-xl hover:text-red-500 transition-colors">
							<Trash2 size={18} />
						</button>
					</div>

					<div class="p-10 flex-1 overflow-y-auto space-y-6">
						<h2 class="text-3xl font-black text-[#1B4B6B] uppercase tracking-tighter leading-tight">{messaggioSelezionato.oggetto}</h2>
						<div class="h-px bg-gray-100 w-20"></div>
						<p class="text-gray-500 font-medium leading-relaxed text-sm whitespace-pre-wrap">{messaggioSelezionato.testo}</p>
					</div>

					<div class="p-8 border-t border-gray-50 flex gap-4 items-center">
						<div class="flex-1 bg-gray-50 rounded-2xl p-2 flex items-center">
							<input type="text" placeholder="RISPOSTA RAPIDA..." class="flex-1 bg-transparent px-4 py-2 text-xs font-bold outline-none" />
							<button class="bg-[#1B4B6B] text-white p-3 rounded-xl hover:scale-105 transition-transform">
								<Send size={16} />
							</button>
						</div>
					</div>
				</div>
			{:else}
				<div class="bg-white rounded-[2.5rem] border border-dashed border-gray-200 h-full flex flex-col items-center justify-center text-center p-10">
					<div class="w-20 h-20 bg-gray-50 rounded-full flex items-center justify-center text-gray-200 mb-6">
						<MessageSquare size={40} />
					</div>
					<h3 class="text-xl font-black text-[#1B4B6B] uppercase italic">Seleziona un messaggio</h3>
					<p class="text-[10px] font-bold text-gray-400 uppercase mt-2 max-w-xs">Clicca su una comunicazione nella lista per leggerne il contenuto</p>
				</div>
			{/if}
		</div>
	</div>
</div>

<style>
    :global(body) { background-color: #F9FAFB; }
    .custom-scrollbar-data::-webkit-scrollbar { width: 4px; }
    .custom-scrollbar-data::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>