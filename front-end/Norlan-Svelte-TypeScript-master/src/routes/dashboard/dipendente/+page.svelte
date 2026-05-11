<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { resolveRoute } from '$app/paths';
	import {
		HardHat, FileBadge, MessageSquare, AlertTriangle,
		CheckCircle2, X, Send, Loader2, ShieldCheck, LayoutDashboard,
		BookOpen, Clock, Calendar
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
		if (statoFormazione === 'WARNING' || statoDPI === 'WARNING') return { label: 'ATTENZIONE', color: 'bg-[#1B4B6B]', icon: Clock };
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

	<div class="mb-8">
		<div class="flex items-center gap-2 text-[#1B4B6B] mb-2">
			<LayoutDashboard size={18} />
			<p class="text-xs font-black uppercase tracking-widest text-gray-500">{dataOggi}</p>
		</div>
		<h1 class="text-3xl md:text-5xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter leading-none">
			Ciao, {utente?.nome || 'Utente'}
		</h1>
		<p class="text-sm font-bold text-gray-400 mt-2 uppercase tracking-wide">La tua area personale NorLan</p>
	</div>

	{#if isLoading}
		<div class="flex flex-col items-center justify-center py-32"><Loader2 size={40} class="animate-spin text-[#1B4B6B]" /></div>
	{:else}
		<div class="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-10">
			<div class="bg-white p-5 rounded-[2rem] border border-gray-100 shadow-sm flex items-center gap-4 relative overflow-hidden">
				<div class="absolute -right-4 -bottom-4 opacity-5 pointer-events-none text-[#1B4B6B]">
					<status.icon size={100} />
				</div>
				<div class="w-14 h-14 rounded-2xl flex items-center justify-center {status.color} text-white shrink-0 shadow-lg relative z-10">
					<status.icon size={24} />
				</div>
				<div class="relative z-10">
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Stato Globale</p>
					<h2 class="text-xl font-black text-[#1B4B6B] uppercase leading-none mt-1">{status.label}</h2>
				</div>
			</div>
			<StatCard titolo="Formazione" valore={statoFormazione === 'OK' ? 'In Regola' : 'Da Svolgere'} icona={statoFormazione === 'OK' ? CheckCircle2 : AlertTriangle} bgIcona={statoFormazione === 'DANGER' ? "bg-red-100" : "bg-[#1B4B6B]/10"} testoIcona={statoFormazione === 'DANGER' ? "text-red-600" : "text-[#1B4B6B]"} href={resolveRoute('/dashboard/dipendente/corsi')} />
			<StatCard titolo="Stato DPI" valore={statoDPI === 'OK' ? 'Verificati' : 'Scaduti'} icona={HardHat} bgIcona={statoDPI === 'DANGER' ? "bg-red-100" : "bg-[#1B4B6B]/10"} testoIcona={statoDPI === 'DANGER' ? "text-red-600" : "text-[#1B4B6B]"} href={resolveRoute('/dashboard/dipendente/dpi')} />
			<StatCard titolo="Attestati" valore={materiali.length} icona={FileBadge} bgIcona="bg-[#1B4B6B]/10" testoIcona="text-[#1B4B6B]" href={resolveRoute('/dashboard/dipendente/attestati')} />
		</div>

		<div class="grid grid-cols-1 lg:grid-cols-12 gap-8">

			<div class="lg:col-span-8 flex flex-col gap-10">

				{#if impegni.length > 0}
					<section>
						<div class="flex justify-between items-center mb-4 px-2">
							<div>
								<div class="flex items-center gap-2 text-[#1B4B6B] mb-1">
									<BookOpen size={16} />
									<span class="text-[10px] font-black uppercase tracking-widest text-gray-500">In Programma</span>
								</div>
								<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter">I tuoi Corsi</h2>
							</div>
							<a href={resolveRoute('/dashboard/dipendente/corsi')} class="text-[9px] font-black text-[#1B4B6B] uppercase hover:underline">
								Tutti
							</a>
						</div>

						<div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
							{#each impegni.slice(0, 4) as imp}
								<div class="bg-gray-50/50 rounded-2xl p-4 border border-gray-100 hover:border-[#1B4B6B]/30 hover:bg-white transition-all group flex flex-col justify-center gap-2">
									<div class="flex items-center gap-2">
										<Calendar size={14} class="text-[#1B4B6B]/70" />
										<span class="text-[10px] font-bold text-[#1B4B6B] uppercase">{imp.data}</span>
									</div>
									<p class="text-xs font-black text-[#1B4B6B] uppercase line-clamp-2 leading-snug" title={imp.titolo}>{imp.titolo}</p>
								</div>
							{/each}
						</div>
					</section>
				{/if}

				{#if materiali.length > 0}
					<section>
						<div class="flex justify-between items-center mb-4 px-2">
							<div>
								<div class="flex items-center gap-2 text-[#1B4B6B] mb-1">
									<FileBadge size={16} />
									<span class="text-[10px] font-black uppercase tracking-widest text-gray-500">Documentazione</span>
								</div>
								<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter">Ultimi Attestati</h2>
							</div>
							<a href={resolveRoute('/dashboard/dipendente/attestati')} class="text-[9px] font-black text-[#1B4B6B] uppercase hover:underline">
								Archivio
							</a>
						</div>
						<div class="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden p-3 space-y-2">
							{#each materiali.slice(0, 4) as mat}
								<div class="flex items-center bg-gray-50/50 p-4 rounded-2xl hover:bg-gray-50 transition-colors border border-transparent hover:border-gray-100 group">
									<div class="flex flex-col min-w-0 w-full">
										<p class="text-xs font-black text-[#1B4B6B] uppercase truncate mb-1" title={mat.titolo}>{mat.titolo}</p>
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
					<section class="bg-white rounded-[2.5rem] p-6 shadow-sm border border-gray-100 relative overflow-hidden h-full flex flex-col">
						<div class="absolute -top-10 -right-10 text-[#1B4B6B]/5 pointer-events-none">
							<HardHat size={200} />
						</div>

						<div class="relative z-10 flex justify-between items-center mb-6 pb-4 border-b border-gray-100">
							<div>
								<div class="flex items-center gap-2 text-[#1B4B6B] mb-1">
									<HardHat size={16} />
									<span class="text-[10px] font-black uppercase tracking-widest text-gray-500">Equipaggiamento</span>
								</div>
								<h2 class="text-xl font-black text-[#1B4B6B] uppercase tracking-tight">DPI Dotazione</h2>
							</div>
							<a href={resolveRoute('/dashboard/dipendente/dpi')} class="text-[9px] font-black text-[#1B4B6B] uppercase hover:underline">
								Tutti
							</a>
						</div>

						<div class="relative z-10 space-y-3 flex-1">
							{#each dotazioniDPI.slice(0, 5) as dpi}
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

<div class="fixed bottom-8 right-8 z-50 flex flex-col items-end">
	{#if isChatOpen}
		<div transition:scale={{ duration: 200, start: 0.9 }} class="mb-4 flex h-96 w-80 flex-col overflow-hidden rounded-[2rem] border border-gray-100 bg-white shadow-2xl">
			<div class="flex items-center justify-between bg-[#1B4B6B] p-5 text-white">
				<span class="text-xs font-black uppercase tracking-widest">Supporto</span>
				<button onclick={() => (isChatOpen = false)}><X size={16} /></button>
			</div>
			<div bind:this={chatScrollContainer} class="flex-1 space-y-3 overflow-y-auto bg-gray-50 p-4 custom-scrollbar">
				{#each messaggiChat as msg}
					<div class="flex {msg.idMittente === currentUser?.idUtente ? 'justify-end' : 'justify-start'}">
						<div class="max-w-[80%] p-3 text-xs rounded-2xl {msg.idMittente === currentUser?.idUtente ? 'rounded-tr-none bg-[#1B4B6B] text-white' : 'rounded-tl-none bg-white text-[#1B4B6B] shadow-sm'}">{msg.testo}</div>
					</div>
				{/each}
			</div>
			<form class="flex gap-2 border-t bg-white p-3" onsubmit={(e) => { e.preventDefault(); inviaMessaggioChat(); }}>
				<input bind:value={chatMessage} type="text" placeholder="Scrivi..." class="flex-1 rounded-xl border-none bg-gray-50 px-4 py-2 text-xs outline-none focus:ring-2 focus:ring-[#1B4B6B]/10" />
				<button type="submit" class="flex h-10 w-10 items-center justify-center rounded-xl bg-[#1B4B6B] text-white"><Send size={14} /></button>
			</form>
		</div>
	{/if}
	<button onclick={() => { isChatOpen = !isChatOpen; if (isChatOpen) setTimeout(scrollChat, 50); }} class="flex h-16 w-16 items-center justify-center rounded-full bg-[#1B4B6B] text-white shadow-2xl hover:scale-110 transition-transform">
		<MessageSquare size={24} />
	</button>
</div>

<style>
	:global(body) { background-color: #F8FAFC; }
	.custom-scrollbar::-webkit-scrollbar { width: 4px; }
	.custom-scrollbar::-webkit-scrollbar-thumb { background: #CBD5E1; border-radius: 10px; }
</style>