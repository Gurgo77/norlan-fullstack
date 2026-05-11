<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { resolveRoute } from '$app/paths';
	import {
		HardHat, FileBadge, MessageSquare, AlertTriangle,
		CheckCircle2, X, Send, Loader2, ShieldCheck, LayoutDashboard,
		BookOpen, Clock, Calendar, ChevronRight
	} from 'lucide-svelte';
	import { AuthService, type UserSession } from '$lib/services/AuthService';
	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { ChatService } from '$lib/services/ChatService';
	import { Messaggio } from '$lib/models/Messaggio';
	import StatCard from '$lib/Components/UI/StatCard.svelte';
	import DettagliDocCard from '$lib/Components/Features/Documentale/DettagliDocCard.svelte';
	import { getInfoScadenza, formattaDataScadenza } from '$lib/utils/scadenzeUtils';

	let isLoading = $state(true);
	let currentUser = $state<UserSession | null>(null);
	let utente = $state<DipendenteDTO | null>(null);
	let isChatOpen = $state(false), chatMessage = $state('');
	let chatService = $state<ChatService | null>(null), messaggiChat = $state<Messaggio[]>([]);
	let chatScrollContainer = $state<HTMLDivElement | null>(null);

	let statoFormazione = $state<'OK' | 'WARNING' | 'DANGER'>('OK');
	let statoDPI = $state<'OK' | 'WARNING' | 'DANGER'>('OK');
	let impegni = $state<any[]>([]), dotazioniDPI = $state<any[]>([]), materiali = $state<any[]>([]);

	const dataOggi = new Date().toLocaleDateString('it-IT', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' });

	const status = $derived.by(() => {
		if (statoDPI === 'DANGER') return { label: 'CRITICO', color: 'bg-red-500', icon: AlertTriangle };
		if (statoFormazione === 'WARNING' || statoDPI === 'WARNING') return { label: 'ATTENZIONE', color: 'bg-amber-400', icon: Clock };
		return { label: 'A NORMA', color: 'bg-[#1B4B6B]', icon: ShieldCheck };
	});

	function scrollChat() { if (chatScrollContainer) setTimeout(() => chatScrollContainer!.scrollTop = chatScrollContainer!.scrollHeight, 50); }

	onMount(async () => {
		currentUser = AuthService.getSession();
		if (!currentUser) return;
		try {
			const [dipData, dpiDataRaw, iscrizioniData, cronologiaChat] = await Promise.all([
				LavoratoreService.getById(currentUser.idUtente),
				LavoratoreService.getDpiByLavoratore(currentUser.idUtente),
				FormazioneService.getIscrizioniUtente(currentUser.idUtente),
				ChatService.getCronologia(currentUser.idUtente, 1)
			]);
			utente = dipData;
			messaggiChat = cronologiaChat;

			dotazioniDPI = (dpiDataRaw as any[]).map(d => {
				const dataScad = d.dataScadenzaRevisione || d.dataScadenza || '';
				const info = getInfoScadenza(dataScad);
				return {
					id: d.idAssegnazione || d.id || 0,
					nome: (d.tipo || d.nomeDpi || 'DPI').replace(/_/g, ' '),
					stato: info.stato,
					revisione: formattaDataScadenza(dataScad),
					ts: dataScad ? new Date(dataScad).getTime() : 0
				};
			}).sort((a, b) => a.ts - b.ts);

			statoDPI = dotazioniDPI.some(d => d.stato === 'DANGER') ? 'DANGER' : dotazioniDPI.some(d => d.stato === 'WARNING') ? 'WARNING' : 'OK';

			let hasCorsi = false;
			iscrizioniData.forEach(i => {
				const statoCorso = i.statoCorso || '';

				if (['PROGRAMMATO', 'IN_SVOLGIMENTO'].includes(statoCorso)) {
					hasCorsi = true;
					impegni.push({
						id: i.idCorso,
						titolo: i.titoloCorso || 'Corso in programma',
						data: formattaDataScadenza(i.dataOrarioCorso || ''),
						ts: i.dataOrarioCorso ? new Date(i.dataOrarioCorso).getTime() : 0
					});
				}
				if (i.presenzaConfermata && i.idDocumento) {
					materiali.push({
						id: i.idCorso,
						titolo: i.titoloCorso || 'Attestato',
						data: formattaDataScadenza(i.dataOrarioCorso || '')
					});
				}
			});
			impegni.sort((a, b) => a.ts - b.ts);
			statoFormazione = hasCorsi ? 'WARNING' : 'OK';

			chatService = new ChatService((msg) => { if (msg.idMittente === 1 || msg.idMittente === currentUser?.idUtente) { messaggiChat = [...messaggiChat, msg]; scrollChat(); } }, console.error);
			chatService.connect(AuthService.getToken()!, currentUser.idUtente);
		} catch (error) { console.error(error); } finally { isLoading = false; setTimeout(scrollChat, 100); }
	});

	onDestroy(() => chatService?.disconnect());

	function inviaMessaggioChat() {
		if (!chatMessage.trim() || !chatService || !currentUser || !utente) return;
		chatService.sendMessage({ idMittente: currentUser.idUtente, idDestinatario: 1, testo: chatMessage });
		messaggiChat = [...messaggiChat, new Messaggio({ idMessaggio: Date.now(), idMittente: currentUser.idUtente, nomeMittente: utente.nome ?? 'Lavoratore', idDestinatario: 1, testo: chatMessage, timestampInvio: new Date().toISOString(), letto: false })];
		chatMessage = ''; scrollChat();
	}
</script>

<div in:fade class="max-w-[1400px] mx-auto pb-24 p-4 md:p-8">

	<div class="mb-8 md:mb-10">
		<div class="flex items-center gap-2 text-[#1B4B6B] mb-2">
			<LayoutDashboard size={18} class="shrink-0" />
			<p class="text-[10px] md:text-xs font-black uppercase tracking-widest text-gray-500">{dataOggi}</p>
		</div>
		<h1 class="text-3xl md:text-5xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter leading-tight">
			Ciao, {utente?.nome || 'Utente'}
		</h1>
		<p class="text-xs md:text-sm font-bold text-gray-400 mt-2 uppercase tracking-wide">La tua area personale NorLan</p>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={40} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black uppercase tracking-widest text-gray-400">Caricamento profilo...</span>
		</div>
	{:else}
		<div class="w-full overflow-x-auto pb-4 -mx-2 px-2 sm:mx-0 sm:px-0 custom-scrollbar-data mb-10">
			<div class="flex lg:grid lg:grid-cols-4 gap-4 min-w-max lg:min-w-0">
				<div class="bg-white p-5 rounded-[2rem] border border-gray-100 shadow-sm flex items-center gap-4 relative overflow-hidden w-64 lg:w-auto">
					<div class="absolute -right-4 -bottom-4 opacity-5 pointer-events-none text-[#1B4B6B]">
						<status.icon size={100} />
					</div>
					<div class="w-12 h-12 md:w-14 md:h-14 rounded-2xl flex items-center justify-center {status.color} text-white shrink-0 shadow-lg relative z-10">
						<status.icon size={24} class={status.label === 'ATTENZIONE' ? 'text-[#1B4B6B]' : 'text-white'} />
					</div>
					<div class="relative z-10 min-w-0">
						<p class="text-[9px] md:text-[10px] font-black text-gray-400 uppercase tracking-widest">Stato Globale</p>
						<h2 class="text-lg md:text-xl font-black text-[#1B4B6B] uppercase leading-none mt-1 truncate">{status.label}</h2>
					</div>
				</div>
				<StatCard titolo="Formazione" valore={statoFormazione === 'OK' ? 'In Regola' : 'Da Svolgere'} icona={statoFormazione === 'OK' ? CheckCircle2 : AlertTriangle} bgIcona={statoFormazione === 'DANGER' ? "bg-red-100" : "bg-[#1B4B6B]/10"} testoIcona={statoFormazione === 'DANGER' ? "text-red-600" : "text-[#1B4B6B]"} href={resolveRoute('/dashboard/dipendente/corsi')} />
				<StatCard titolo="Stato DPI" valore={statoDPI === 'OK' ? 'Verificati' : 'Scaduti'} icona={HardHat} bgIcona={statoDPI === 'DANGER' ? "bg-red-100" : "bg-[#1B4B6B]/10"} testoIcona={statoDPI === 'DANGER' ? "text-red-600" : "text-[#1B4B6B]"} href={resolveRoute('/dashboard/dipendente/dpi')} />
				<StatCard titolo="Attestati" valore={materiali.length} icona={FileBadge} bgIcona="bg-[#1B4B6B]/10" testoIcona="text-[#1B4B6B]" href={resolveRoute('/dashboard/dipendente/attestati')} />
			</div>
		</div>

		<div class="grid grid-cols-1 lg:grid-cols-12 gap-8">

			<div class="lg:col-span-8 flex flex-col gap-8 md:gap-10">

				{#if impegni.length > 0}
					<section>
						<div class="flex justify-between items-center mb-6 px-2">
							<div class="min-w-0">
								<div class="flex items-center gap-2 text-[#1B4B6B] mb-1">
									<BookOpen size={16} class="shrink-0" />
									<span class="text-[10px] font-black uppercase tracking-widest text-gray-500">In Programma</span>
								</div>
								<h2 class="text-xl md:text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter truncate">I tuoi Corsi</h2>
							</div>
							<a href={resolveRoute('/dashboard/dipendente/corsi')} class="text-[10px] font-black text-[#1B4B6B] uppercase hover:gap-2 transition-all flex items-center gap-1 shrink-0">
								Tutti <ChevronRight size={14} />
							</a>
						</div>

						<div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
							{#each impegni.slice(0, 4) as imp}
								<div class="bg-gray-50/50 rounded-2xl p-5 border border-gray-100 hover:border-[#1B4B6B]/30 hover:bg-white transition-all group flex flex-col justify-center gap-2 shadow-sm">
									<div class="flex items-center gap-2">
										<Calendar size={14} class="text-[#1B4B6B]/70" />
										<span class="text-[10px] font-bold text-[#1B4B6B] uppercase">{imp.data}</span>
									</div>
									<p class="text-sm font-black text-[#1B4B6B] uppercase line-clamp-2 leading-snug" title={imp.titolo}>{imp.titolo}</p>
								</div>
							{/each}
						</div>
					</section>
				{/if}

				{#if materiali.length > 0}
					<section>
						<div class="flex justify-between items-center mb-6 px-2">
							<div class="min-w-0">
								<div class="flex items-center gap-2 text-[#1B4B6B] mb-1">
									<FileBadge size={16} class="shrink-0" />
									<span class="text-[10px] font-black uppercase tracking-widest text-gray-500">Documentazione</span>
								</div>
								<h2 class="text-xl md:text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter truncate">Ultimi Attestati</h2>
							</div>
							<a href={resolveRoute('/dashboard/dipendente/attestati')} class="text-[10px] font-black text-[#1B4B6B] uppercase hover:gap-2 transition-all flex items-center gap-1 shrink-0">
								Archivio <ChevronRight size={14} />
							</a>
						</div>
						<div class="bg-white rounded-[2rem] shadow-sm border border-gray-100 overflow-hidden p-3 space-y-2">
							{#each materiali.slice(0, 4) as mat}
								<div class="flex items-center bg-gray-50/50 p-4 rounded-2xl hover:bg-gray-50 transition-colors border border-transparent hover:border-gray-100 group">
									<div class="flex flex-col min-w-0 w-full">
										<p class="text-sm font-black text-[#1B4B6B] uppercase truncate mb-1" title={mat.titolo}>{mat.titolo}</p>
										<p class="text-[10px] text-gray-400 font-bold uppercase">{mat.data}</p>
									</div>
								</div>
							{/each}
						</div>
					</section>
				{/if}

			</div>

			<div class="lg:col-span-4">
				{#if dotazioniDPI.length > 0}
					<section class="bg-white rounded-[2.5rem] p-6 shadow-sm border border-gray-100 relative overflow-hidden h-full flex flex-col min-h-[400px]">
						<div class="absolute -top-10 -right-10 text-[#1B4B6B]/5 pointer-events-none">
							<HardHat size={200} />
						</div>

						<div class="relative z-10 flex justify-between items-center mb-6 pb-4 border-b border-gray-100">
							<div>
								<div class="flex items-center gap-2 text-[#1B4B6B] mb-1">
									<HardHat size={16} class="shrink-0" />
									<span class="text-[10px] font-black uppercase tracking-widest text-gray-500">Equipaggiamento</span>
								</div>
								<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tight">DPI Dotazione</h2>
							</div>
							<a href={resolveRoute('/dashboard/dipendente/dpi')} class="text-[10px] font-black text-[#1B4B6B] uppercase shrink-0">
								Tutti
							</a>
						</div>

						<div class="relative z-10 space-y-3 flex-1">
							{#each dotazioniDPI.slice(0, 6) as dpi}
								<div class="bg-gray-50/50 rounded-2xl p-1 border border-gray-100 hover:border-[#1B4B6B]/20 transition-colors">
									<DettagliDocCard tipo="DPI" titolo={dpi.nome} dataScadenza={dpi.revisione} />
								</div>
							{/each}
						</div>
					</section>
				{/if}
			</div>

		</div>
	{/if}
</div>

<div class="fixed bottom-6 right-6 md:bottom-8 md:right-8 z-50 flex flex-col items-end">
	{#if isChatOpen}
		<div transition:scale={{ duration: 200, start: 0.9 }} class="mb-4 flex h-[450px] w-[calc(100vw-3rem)] sm:w-80 flex-col overflow-hidden rounded-[2rem] border border-gray-100 bg-white shadow-2xl">
			<div class="flex items-center justify-between bg-[#1B4B6B] p-5 text-white">
				<div class="flex items-center gap-2">
					<div class="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
					<span class="text-xs font-black uppercase tracking-widest">Supporto NorLan</span>
				</div>
				<button onclick={() => (isChatOpen = false)} class="p-1 hover:bg-white/10 rounded-lg transition-colors"><X size={18} /></button>
			</div>
			<div bind:this={chatScrollContainer} class="flex-1 space-y-4 overflow-y-auto bg-gray-50 p-4 custom-scrollbar">
				{#each messaggiChat as msg}
					<div class="flex {msg.idMittente === currentUser?.idUtente ? 'justify-end' : 'justify-start'}">
						<div class="max-w-[85%] p-3.5 text-xs font-medium rounded-2xl {msg.idMittente === currentUser?.idUtente ? 'rounded-tr-none bg-[#1B4B6B] text-white shadow-md' : 'rounded-tl-none bg-white text-[#1B4B6B] shadow-sm border border-gray-100'}">
							{msg.testo}
						</div>
					</div>
				{/each}
			</div>
			<form class="flex gap-2 border-t border-gray-100 bg-white p-4" onsubmit={(e) => { e.preventDefault(); inviaMessaggioChat(); }}>
				<input bind:value={chatMessage} type="text" placeholder="Scrivi..." class="flex-1 rounded-xl border-none bg-gray-100 px-4 py-2.5 text-xs outline-none focus:ring-2 focus:ring-[#1B4B6B]/20 transition-all font-bold" />
				<button type="submit" disabled={!chatMessage.trim()} class="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#1B4B6B] text-white shadow-md disabled:opacity-50 transition-all active:scale-95"><Send size={16} /></button>
			</form>
		</div>
	{/if}
	<button onclick={() => { isChatOpen = !isChatOpen; if (isChatOpen) setTimeout(scrollChat, 50); }} class="flex h-14 w-14 md:h-16 md:w-16 items-center justify-center rounded-full bg-[#1B4B6B] text-white shadow-2xl hover:scale-110 active:scale-95 transition-all">
		<MessageSquare size={24} />
	</button>
</div>

<style>
	:global(body) { background-color: #F8FAFC; }
	.custom-scrollbar::-webkit-scrollbar { width: 4px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 10px; }
	.custom-scrollbar-data::-webkit-scrollbar { height: 4px; }
	.custom-scrollbar-data::-webkit-scrollbar-track { background: transparent; }
	.custom-scrollbar-data::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>