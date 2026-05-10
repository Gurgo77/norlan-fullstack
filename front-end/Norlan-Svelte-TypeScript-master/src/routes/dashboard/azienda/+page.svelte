<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		ShieldCheck, ShieldOff, MessageSquare, Clock,
		AlertTriangle, CheckCircle2, X, Send, Loader2, User, Users,
		Building2, Briefcase, BellRing, FileText, PlayCircle, ArrowRight
	} from 'lucide-svelte';
	import { AuthService, type UserSession } from '$lib/services/AuthService';
	import { AnagraficaService } from '$lib/services/AnagraficaService';
	import { DocumentoService } from '$lib/services/DocumentoService';
	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';
	import { FormazioneService } from '$lib/services/FormazioneService';
	import { ChatService } from '$lib/services/ChatService';
	import type { AziendaData } from '$lib/models/Azienda';
	import type { Documento } from '$lib/models/Documento';
	import type { CorsoFormazione } from '$lib/models/CorsoFormazione';
	import { Messaggio } from '$lib/models/Messaggio';
	import { TipoDocumento } from '$lib/models/Enums';
	import StatCard from '$lib/Components/UI/StatCard.svelte';
	import AlertCard from '$lib/Components/UI/AlertCard.svelte';
	import DashboardCorsoCard from '$lib/Components/Features/Formazione/DashboardCorsoCard.svelte';

	import { getInfoScadenza, formattaDataScadenza, calcolaGiorniRimanenti } from '$lib/utils/scadenzeUtils';

	let isLoading = $state(true);
	let currentUser = $state<UserSession | null>(null);
	let utenteAzienda = $state<AziendaData | null>(null);
	let isChatOpen = $state(false);
	let chatMessage = $state('');
	let chatService = $state<ChatService | null>(null);
	let messaggiChat = $state<Messaggio[]>([]);
	let chatScrollContainer = $state<HTMLDivElement | null>(null);
	const STAFF_ID = 1;

	let documentiScadenza = $state<Documento[]>([]);
	let dipendenti = $state<DipendenteDTO[]>([]);
	let prossimiCorsi = $state<CorsoFormazione[]>([]);

	const dataOggi = new Intl.DateTimeFormat('it-IT', {
		weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
	}).format(new Date());

	const alertScadenzeDocs = $derived(
			documentiScadenza.filter(d => calcolaGiorniRimanenti(d.dataScadenza) <= 30)
	);

	const infoStato = $derived(() => {
		const haDocsScaduti = documentiScadenza.some(d => getInfoScadenza(d.dataScadenza).stato === 'DANGER');

		if (haDocsScaduti) return {
			label: 'CRITICO',
			color: 'bg-red-500',
			icon: AlertTriangle,
			text: 'Documenti Scaduti Rilevati'
		};

		if (alertScadenzeDocs.length > 0) return {
			label: 'ATTENZIONE',
			color: 'bg-amber-400 text-[#1B4B6B]',
			icon: Clock,
			text: 'Scadenze Imminenti'
		};

		return {
			label: 'A NORMA',
			color: 'bg-emerald-500',
			icon: ShieldCheck,
			text: 'Nessuna criticità rilevata'
		};
	});

	const status = $derived(infoStato());

	function scaricaReport(idCorso: number | string) {
		alert("Download report formazione corso ID: " + idCorso);
	}

	function scrollChat() {
		setTimeout(() => { if (chatScrollContainer) chatScrollContainer.scrollTop = chatScrollContainer.scrollHeight; }, 50);
	}

	onMount(async () => {
		currentUser = AuthService.getSession();
		const token = AuthService.getToken();
		if (!currentUser || !token) return;
		try {
			const profilo = await AnagraficaService.getAziendaById(currentUser.idUtente) as AziendaData;
			utenteAzienda = profilo;
			const [docs, lavoratori, corsi, cronologiaChat] = await Promise.all([
				DocumentoService.getDocumentiByAzienda(currentUser.idUtente),
				LavoratoreService.getByAzienda(currentUser.idUtente),
				FormazioneService.getAllCorsi(),
				ChatService.getCronologia(currentUser.idUtente, STAFF_ID)
			]);
			documentiScadenza = docs.filter(doc => doc.tipologia !== TipoDocumento.ATTESTATO_CORSO);
			dipendenti = lavoratori;
			prossimiCorsi = corsi.filter(c => c.stato === 'PROGRAMMATO' || !c.stato).slice(0, 4);
			messaggiChat = cronologiaChat;

			chatService = new ChatService(
					(msg: Messaggio) => {
						if (msg.idMittente === STAFF_ID || msg.idMittente === currentUser?.idUtente) {
							messaggiChat = [...messaggiChat, msg];
							scrollChat();
						}
					},
					(err: string) => console.error(err)
			);
			chatService.connect(token, currentUser.idUtente);
		} catch (error) { console.error(error); } finally { isLoading = false; setTimeout(scrollChat, 100); }
	});

	onDestroy(() => { if (chatService) chatService.disconnect(); });

	function inviaMessaggioChat() {
		if (!chatMessage.trim() || !currentUser || !chatService) return;
		chatService.sendMessage({ idMittente: currentUser.idUtente, idDestinatario: STAFF_ID, testo: chatMessage });
		const msgMock = new Messaggio({ idMessaggio: Date.now(), idMittente: currentUser.idUtente, nomeMittente: currentUser.email, idDestinatario: STAFF_ID, testo: chatMessage, timestampInvio: new Date().toISOString(), letto: false });
		messaggiChat = [...messaggiChat, msgMock]; chatMessage = ''; scrollChat();
	}
</script>

<div in:fade class="max-w-[1600px] mx-auto space-y-8 pb-10">
	<div class="flex items-end justify-between">
		<div>
			<div class="flex items-center gap-3 mb-2">
				<div class="p-2 bg-[#1B4B6B] rounded-xl text-white shadow-sm"><Building2 size={20} /></div>
				<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">{dataOggi}</p>
			</div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">{utenteAzienda?.ragioneSociale || 'Area Azienda'}</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">Pannello di controllo aziendale</p>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Sincronizzazione Dati NorLan...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
			<div class="lg:col-span-2 bg-white p-8 rounded-[2.5rem] shadow-sm border border-gray-100 flex items-center gap-8 group cursor-default">
				<div class="relative shrink-0">
					<div class="w-20 h-20 rounded-full border-4 border-white flex items-center justify-center {status.color} shadow-xl text-white">
						<status.icon size={32} class={status.label === 'ATTENZIONE' ? 'text-[#1B4B6B]' : 'text-white'} />
					</div>
				</div>
				<div>
					<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">Status Compliance</p>
					<h2 class="text-3xl font-black text-[#1B4B6B] uppercase leading-none">{status.label}</h2>
					<p class="text-xs font-bold text-gray-400 uppercase mt-2">{status.text}</p>
				</div>
			</div>
			<div class="grid grid-rows-2 gap-4">
				<StatCard titolo="Personale" valore="{dipendenti.length} Censiti" icona={Users} href="/dashboard/azienda/dipendenti" />
				<StatCard titolo="Archivio" valore="Documenti" icona={FileText} href="/dashboard/azienda/documenti" />
			</div>
		</div>

		<div class="grid grid-cols-1 xl:grid-cols-3 gap-8">
			<div class="xl:col-span-2 space-y-8">
				<div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-8">
					<div class="flex items-center justify-between mb-6 border-b border-gray-50 pb-4">
						<h3 class="text-lg font-black text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-2">
							<BellRing size={20} class="text-amber-500" /> Scadenze Documentali
						</h3>
						<span class="bg-gray-100 text-gray-500 text-[9px] font-black px-3 py-1 rounded-full uppercase">{alertScadenzeDocs.length} Alert</span>
					</div>
					<div class="space-y-3">
						{#each alertScadenzeDocs as doc (doc.idDocumento)}
							{@const info = getInfoScadenza(doc.dataScadenza)}
							<AlertCard
									titolo={doc.tipologia.replace(/_/g, ' ')}
									sottotitolo={doc.modulo}
									variante={info.stato === 'DANGER' ? 'danger' : 'warning'}
									icona={info.icona}
									stato={info.label.toUpperCase()}
									data={formattaDataScadenza(doc.dataScadenza)}
									href="/dashboard/azienda/documenti"
							/>
						{/each}
						{#if alertScadenzeDocs.length === 0}
							<div class="py-6 text-center">
								<CheckCircle2 size={32} class="mx-auto text-emerald-400 mb-2 opacity-50" />
								<p class="text-[10px] font-black uppercase tracking-widest text-gray-400">Tutti i documenti aziendali sono validi.</p>
							</div>
						{/if}
					</div>
				</div>

				<div class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm p-8">
					<div class="flex items-center justify-between mb-6 border-b border-gray-50 pb-4">
						<h3 class="text-lg font-black text-[#1B4B6B] uppercase tracking-tighter flex items-center gap-2">
							<PlayCircle size={20} class="text-blue-500" /> Formazione in Evidenza
						</h3>
						<a href="/dashboard/azienda/formazione" class="text-[9px] font-black uppercase text-[#1B4B6B] hover:underline flex items-center gap-1">Gestisci Tutti <ArrowRight size={12}/></a>
					</div>
					<div class="grid grid-cols-1 md:grid-cols-2 gap-4">
						{#each prossimiCorsi as corso (corso.idCorso)}
							<DashboardCorsoCard
									ruolo="azienda"
									corso={{
                            id: corso.idCorso,
                            titolo: corso.titolo,
                            stato: 'DA_INIZIARE',
                            dataSvolgimento: formattaDataScadenza(corso.dataOrario),
                            luogo: corso.luogoFisico
                         }}
									onAzioneCorso={() => scaricaReport(corso.idCorso)}
							/>
						{/each}
					</div>
					{#if prossimiCorsi.length === 0}
						<div class="py-10 text-center">
							<p class="text-[10px] font-black uppercase tracking-widest text-gray-400 italic">Nessun corso in programma.</p>
						</div>
					{/if}
				</div>
			</div>

			<div class="space-y-8">
				<div class="bg-white rounded-[2.5rem] p-8 border border-gray-100 shadow-sm flex flex-col items-center text-center">
					<div class="w-16 h-16 rounded-[1.5rem] bg-gray-100 flex items-center justify-center mb-4 text-[#1B4B6B]"><Briefcase size={28} /></div>
					<h4 class="text-sm font-black text-[#1B4B6B] uppercase">{utenteAzienda?.ragioneSociale}</h4>
					<p class="text-[10px] font-bold text-gray-400 uppercase mt-1 mb-1">P.IVA: {utenteAzienda?.partitaIva}</p>
					<div class="w-full border-t border-gray-50 my-4"></div>
					<div class="flex items-center gap-2 text-[10px] font-bold text-gray-500 uppercase mb-6"><User size={12} class="text-[#1B4B6B]" /> {utenteAzienda?.referenteAziendale || 'Referente N.D.'}</div>
					<a href="/dashboard/azienda/account" class="w-full py-3 bg-[#1B4B6B] text-white rounded-xl text-[10px] font-black uppercase hover:bg-[#153a54] transition-all shadow-lg shadow-blue-900/10">Modifica Profilo</a>
				</div>
			</div>
		</div>
	{/if}
</div>