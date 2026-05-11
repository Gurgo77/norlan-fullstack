<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		ShieldCheck, Clock,
		AlertTriangle, CheckCircle2, Loader2, User, Users,
		Building2, Briefcase, BellRing, FileText, PlayCircle, ArrowRight, HardHat
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
	let chatService = $state<ChatService | null>(null);
	let messaggiChat = $state<Messaggio[]>([]);
	let chatScrollContainer = $state<HTMLDivElement | null>(null);
	const STAFF_ID = 1;

	let documentiScadenza = $state<Documento[]>([]);
	let dipendenti = $state<DipendenteDTO[]>([]);
	let prossimiCorsi = $state<CorsoFormazione[]>([]);
	let dpisAzienda = $state<any[]>([]);

	const dataOggi = new Date().toLocaleDateString('it-IT', {
		weekday: 'long', day: '2-digit', month: 'short', year: 'numeric'
	});

	const alertScadenzeDocs = $derived(
			documentiScadenza.filter(d => calcolaGiorniRimanenti(d.dataScadenza) <= 30)
	);

	const alertDpis = $derived(
			dpisAzienda.filter(dpi => {
				const info = getInfoScadenza(dpi.dataScadenzaRevisione || dpi.dataScadenza);
				return info.stato === 'DANGER' || info.stato === 'WARNING';
			})
	);

	const infoStato = $derived(() => {
		const haDocsScaduti = documentiScadenza.some(d => getInfoScadenza(d.dataScadenza).stato === 'DANGER');
		const haDpiScaduti = alertDpis.some(dpi => getInfoScadenza(dpi.dataScadenzaRevisione || dpi.dataScadenza).stato === 'DANGER');

		if (haDocsScaduti || haDpiScaduti) return {
			label: 'CRITICO',
			color: 'bg-red-500',
			icon: AlertTriangle,
			text: 'Scadenze Critiche Rilevate'
		};

		if (alertScadenzeDocs.length > 0 || alertDpis.length > 0) return {
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

			const dpiPromises = lavoratori.map((dip: any) =>
					LavoratoreService.getDpiByLavoratore(dip.idUtente)
							.then(dpis => dpis.map((d: any) => ({ ...d, nomeDipendente: `${dip.nome} ${dip.cognome}` })))
							.catch(() => [])
			);
			const dpiResults = await Promise.all(dpiPromises);
			dpisAzienda = dpiResults.flat();

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

</script>

<div in:fade class="max-w-7xl mx-auto space-y-8 pb-20 p-6">

	<div class="mb-10">
		<div class="flex items-center gap-3 mb-3">
			<div class="p-2 bg-[#1B4B6B] rounded-xl text-white shadow-sm"><Building2 size={20} /></div>
			<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest">{dataOggi}</p>
		</div>
		<h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">
			{utenteAzienda?.ragioneSociale || 'Area Azienda'}
		</h1>
		<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter mt-1">Pannello di controllo stato compliance.</p>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Sincronizzazione Dati NorLan...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-3 xl:grid-cols-5 gap-6 mb-14">
			<div class="bg-white p-6 rounded-3xl border border-gray-100 shadow-sm flex items-center gap-5 transition-all hover:shadow-md">
				<div class="w-14 h-14 rounded-2xl flex items-center justify-center {status.color} shadow-lg text-white shrink-0">
					<status.icon size={24} class={status.label === 'ATTENZIONE' ? 'text-[#1B4B6B]' : 'text-white'} />
				</div>
				<div>
					<p class="text-[9px] font-black text-gray-400 uppercase tracking-widest mb-1">Compliance</p>
					<h2 class="text-xl font-black text-[#1B4B6B] uppercase leading-none tracking-tight">{status.label}</h2>
				</div>
			</div>

			<StatCard
					titolo="Alert Documenti"
					valore={alertScadenzeDocs.length}
					icona={BellRing}
					bgIcona="bg-[#1B4B6B]/10"
					testoIcona="text-[#1B4B6B]"
					href="/dashboard/azienda/documenti"
			/>
			<StatCard
					titolo="Forza Lavoro"
					valore={dipendenti.length}
					icona={Users}
					bgIcona="bg-[#1B4B6B]/10"
					testoIcona="text-[#1B4B6B]"
					href="/dashboard/azienda/dipendenti"
			/>
			<StatCard
					titolo="DPI Assegnati"
					valore={dpisAzienda.length}
					icona={HardHat}
					bgIcona="bg-[#1B4B6B]/10"
					testoIcona="text-[#1B4B6B]"
					href="/dashboard/azienda/dpi"
			/>
			<StatCard
					titolo="Corsi in Arrivo"
					valore={prossimiCorsi.length}
					icona={PlayCircle}
					bgIcona="bg-[#1B4B6B]/10"
					testoIcona="text-[#1B4B6B]"
					href="/dashboard/azienda/formazione"
			/>
		</div>

		<div class="mb-14">
			<div class="flex items-center justify-between mb-6 border-b border-gray-200 pb-3">
				<div class="flex items-center gap-3">
					<div class="p-2 bg-amber-100 text-amber-600 rounded-lg"><AlertTriangle size={20}/></div>
					<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Scadenze Documentali</h2>
				</div>
				<a href="/dashboard/azienda/documenti" class="text-[10px] font-black uppercase text-gray-400 hover:text-[#1B4B6B] transition-colors flex items-center gap-1">Vedi tutte <ArrowRight size={14}/></a>
			</div>

			{#if alertScadenzeDocs.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center flex flex-col items-center">
					<CheckCircle2 size={32} class="text-emerald-400 mb-2 opacity-50" />
					<p class="text-gray-400 font-bold uppercase text-xs">Tutti i documenti aziendali sono in regola.</p>
				</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each alertScadenzeDocs as doc (doc.idDocumento)}
						{@const info = getInfoScadenza(doc.dataScadenza)}
						<div in:scale>
							<AlertCard
									titolo={doc.tipologia.replace(/_/g, ' ')}
									sottotitolo={doc.modulo}
									variante={info.stato === 'DANGER' ? 'danger' : 'warning'}
									icona={info.icona}
									stato={info.label.toUpperCase()}
									data={formattaDataScadenza(doc.dataScadenza)}
									href="/dashboard/azienda/documenti"
							/>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<div class="mb-14">
			<div class="flex items-center justify-between mb-6 border-b border-gray-200 pb-3">
				<div class="flex items-center gap-3">
					<div class="p-2 bg-orange-100 text-orange-600 rounded-lg"><HardHat size={20}/></div>
					<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Stato Dispositivi DPI</h2>
				</div>
				<a href="/dashboard/azienda/dpi" class="text-[10px] font-black uppercase text-gray-400 hover:text-[#1B4B6B] transition-colors flex items-center gap-1">Gestisci Assegnazioni <ArrowRight size={14}/></a>
			</div>

			{#if alertDpis.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center flex flex-col items-center">
					<CheckCircle2 size={32} class="text-emerald-400 mb-2 opacity-50" />
					<p class="text-gray-400 font-bold uppercase text-xs">Tutti i DPI assegnati sono in regola.</p>
				</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-6">
					{#each alertDpis as dpi (dpi.id || dpi.idAssegnazione || Math.random())}
						{@const info = getInfoScadenza(dpi.dataScadenzaRevisione || dpi.dataScadenza)}
						{@const nomeDpi = (dpi.tipo === 'ALTRO' && dpi.nomeDpi) ? dpi.nomeDpi : (dpi.tipo || 'DPI').replace(/_/g, ' ')}
						<div in:scale>
							<AlertCard
									titolo={nomeDpi}
									sottotitolo={dpi.nomeDipendente}
									variante={info.stato === 'DANGER' ? 'danger' : 'warning'}
									icona={HardHat}
									stato={info.label.toUpperCase()}
									data={formattaDataScadenza(dpi.dataScadenzaRevisione || dpi.dataScadenza)}
									href="/dashboard/azienda/dpi"
							/>
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<div class="mb-14">
			<div class="flex items-center justify-between mb-6 border-b border-gray-200 pb-3">
				<div class="flex items-center gap-3">
					<div class="p-2 bg-emerald-100 text-emerald-600 rounded-lg"><PlayCircle size={20}/></div>
					<h2 class="text-xl font-extrabold text-[#1B4B6B] uppercase tracking-tight">Formazione in Programma</h2>
				</div>
				<a href="/dashboard/azienda/formazione" class="text-[10px] font-black uppercase text-gray-400 hover:text-[#1B4B6B] transition-colors flex items-center gap-1">Gestisci Formazione <ArrowRight size={14}/></a>
			</div>

			{#if prossimiCorsi.length === 0}
				<div class="p-8 border-2 border-dashed border-gray-200 rounded-2xl text-center flex flex-col items-center">
					<CheckCircle2 size={32} class="text-emerald-400 mb-2 opacity-50" />
					<p class="text-gray-400 font-bold uppercase text-xs">Nessun corso programmato al momento.</p>
				</div>
			{:else}
				<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-6">
					{#each prossimiCorsi as corso (corso.idCorso)}
						<div in:scale>
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
						</div>
					{/each}
				</div>
			{/if}
		</div>

		<div class="bg-white rounded-[2.5rem] p-8 border border-gray-100 shadow-sm flex flex-col md:flex-row items-center justify-between gap-6">
			<div class="flex items-center gap-6">
				<div class="w-16 h-16 rounded-[1.5rem] bg-gray-50 flex items-center justify-center text-[#1B4B6B] shrink-0 border border-gray-100">
					<Briefcase size={28} />
				</div>
				<div>
					<h4 class="text-lg font-black text-[#1B4B6B] uppercase tracking-tight">{utenteAzienda?.ragioneSociale}</h4>
					<div class="flex flex-wrap items-center gap-4 mt-2">
						<p class="text-[10px] font-bold text-gray-400 uppercase flex items-center gap-1"><FileText size={12}/> P.IVA: {utenteAzienda?.partitaIva}</p>
						<p class="text-[10px] font-bold text-gray-400 uppercase flex items-center gap-1"><User size={12} class="text-[#1B4B6B]" /> {utenteAzienda?.referenteAziendale || 'Referente N.D.'}</p>
					</div>
				</div>
			</div>
			<a href="/dashboard/azienda/account" class="w-full md:w-auto py-3 px-8 bg-[#1B4B6B] text-white rounded-xl text-[10px] font-black uppercase hover:bg-[#153a54] transition-all shadow-lg shadow-blue-900/10 whitespace-nowrap text-center">
				Modifica Profilo
			</a>
		</div>
	{/if}
</div>