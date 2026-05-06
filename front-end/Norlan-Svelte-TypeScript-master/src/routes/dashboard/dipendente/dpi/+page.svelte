<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		HardHat, ShieldCheck, ShieldAlert, ShieldOff,
		Search, Loader2, RefreshCw
	} from 'lucide-svelte';

	import { AuthService } from '$lib/services/AuthService';
	import { LavoratoreService, type DipendenteDTO } from '$lib/services/LavoratoreService';

	type ComplianceStatus = 'OK' | 'WARNING' | 'DANGER';
	type OpzioneFiltro = 'TUTTI' | ComplianceStatus;

	interface Dpi {
		id: number;
		nome: string;
		matricola: string;
		stato: ComplianceStatus;
		revisione: string;
	}

	interface DpiBackendData {
		idAssegnazione?: number;
		id?: number;
		tipo?: string;
		nomeDpi?: string;
		dataScadenzaRevisione?: string;
		dataScadenza?: string;
	}

	let isLoading = $state(true);
	let searchQuery = $state('');
	let filtroStato = $state<OpzioneFiltro>('TUTTI');

	let dotazioni = $state<Dpi[]>([]);
	let utente = $state<DipendenteDTO | null>(null);

	const opzioniFiltro: OpzioneFiltro[] = ['TUTTI', 'OK', 'WARNING', 'DANGER'];

	function impostaFiltro(valore: OpzioneFiltro) {
		filtroStato = valore;
	}

	function getStatusConfig(stato: ComplianceStatus) {
		const configs = {
			OK: { color: 'text-emerald-500', bg: 'bg-emerald-50', icon: ShieldCheck, label: 'REGOLARE' },
			WARNING: { color: 'text-amber-500', bg: 'bg-amber-50', icon: ShieldAlert, label: 'IN SCADENZA' },
			DANGER: { color: 'text-red-500', bg: 'bg-red-50', icon: ShieldOff, label: 'SCADUTO' }
		};
		return configs[stato];
	}

	function calcolaStato(dataScadenzaStr: string | undefined | null): ComplianceStatus {
		if (!dataScadenzaStr) return 'OK';

		const oggi = new Date().getTime();
		const scadenza = new Date(dataScadenzaStr).getTime();
		const diffGiorni = Math.ceil((scadenza - oggi) / (1000 * 3600 * 24));

		if (diffGiorni < 0) return 'DANGER';
		if (diffGiorni <= 30) return 'WARNING';
		return 'OK';
	}

	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			const [dipendenteData, dpiDataRaw] = await Promise.all([
				LavoratoreService.getById(session.idUtente),
				LavoratoreService.getDpiByLavoratore(session.idUtente)
			]);

			utente = dipendenteData;

			const dpiData = dpiDataRaw as unknown as DpiBackendData[];

			dotazioni = dpiData.map((d: DpiBackendData) => {
				const idReale = d.idAssegnazione || d.id || Date.now();
				const nomeReale = d.tipo ? d.tipo.replace(/_/g, ' ') : (d.nomeDpi || 'SCONOSCIUTO');
				const dataReale = d.dataScadenzaRevisione || d.dataScadenza;

				return {
					id: idReale,
					nome: nomeReale,
					matricola: `DPI-${idReale}`,
					stato: calcolaStato(dataReale),
					revisione: dataReale ? new Date(dataReale).toLocaleDateString('it-IT') : 'NON PREVISTA'
				};
			});

		} catch (error) {
			console.error("Errore durante il recupero dei DPI:", error);
		} finally {
			isLoading = false;
		}
	});

	const dpiFiltrati = $derived(
			dotazioni.filter(d => {
				const searchSafe = searchQuery ? searchQuery.toLowerCase() : '';
				const nomeSafe = d.nome ? d.nome.toLowerCase() : '';
				const matricolaSafe = d.matricola ? d.matricola.toLowerCase() : '';

				const matchSearch = nomeSafe.includes(searchSafe) || matricolaSafe.includes(searchSafe);
				const matchFiltro = filtroStato === 'TUTTI' || d.stato === filtroStato;
				return matchSearch && matchFiltro;
			})
	);

	const conteggi = $derived({
		critici: dotazioni.filter(d => d.stato === 'DANGER').length,
		attenzione: dotazioni.filter(d => d.stato === 'WARNING').length
	});
</script>

<div in:fade class="max-w-7xl mx-auto space-y-8 pb-10">

	<div class="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-8">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">I Miei DPI</h1>
			<p class="text-gray-400 font-bold uppercase text-[10px] tracking-widest mt-1">
				Utente: <span class="text-[#1B4B6B]">{utente?.nome ?? '...'} {utente?.cognome ?? ''}</span> | Gestione dotazioni e scadenze
			</p>
		</div>

		<div class="flex gap-4">
			<div class="bg-white px-6 py-4 rounded-2xl border border-gray-100 shadow-sm flex items-center gap-4">
				<div class="w-10 h-10 bg-red-50 text-red-500 rounded-xl flex items-center justify-center font-black">{conteggi.critici}</div>
				<span class="text-[9px] font-black text-gray-400 uppercase tracking-widest leading-tight">Scaduti</span>
			</div>
			<div class="bg-white px-6 py-4 rounded-2xl border border-gray-100 shadow-sm flex items-center gap-4">
				<div class="w-10 h-10 bg-amber-50 text-amber-500 rounded-xl flex items-center justify-center font-black">{conteggi.attenzione}</div>
				<span class="text-[9px] font-black text-gray-400 uppercase tracking-widest leading-tight">In Scadenza</span>
			</div>
		</div>
	</div>

	<div class="flex flex-col md:flex-row gap-4">
		<div class="relative flex-1 group">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={20} />
			<input
					bind:value={searchQuery}
					type="text"
					placeholder="Cerca per nome o matricola..."
					class="w-full pl-12 pr-6 py-4 bg-white border border-gray-100 rounded-[1.5rem] text-xs font-bold uppercase outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 shadow-sm transition-all"
			/>
		</div>

		<div class="flex bg-gray-100 p-1.5 rounded-[1.5rem] gap-1 overflow-x-auto">
			{#each opzioniFiltro as opzione (opzione)}
				<button
						onclick={() => impostaFiltro(opzione)}
						class="px-5 py-2.5 rounded-xl text-[9px] font-black uppercase tracking-widest transition-all whitespace-nowrap
                {filtroStato === opzione ? 'bg-[#1B4B6B] text-white shadow-md' : 'text-gray-400 hover:text-[#1B4B6B]'}"
				>
					{opzione}
				</button>
			{/each}
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Sincronizzazione inventario DPI...</span>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 gap-8">
			{#each dpiFiltrati as dpi (dpi.id)}
				{@const config = getStatusConfig(dpi.stato)}
				<div in:scale={{duration: 300}} class="bg-white rounded-[2.5rem] border border-gray-100 shadow-sm overflow-hidden flex flex-col hover:shadow-xl transition-all group">
					<div class="p-8 flex items-start gap-6">
						<div class="w-20 h-20 rounded-[1.8rem] {config.bg} {config.color} flex items-center justify-center shrink-0 shadow-inner">
							<config.icon size={32} />
						</div>

						<div class="flex-1 min-w-0">
							<div class="flex justify-between items-start mb-2">
                         <span class="text-[8px] font-black uppercase tracking-[0.2em] px-2 py-1 rounded-md {config.bg} {config.color}">
                            {config.label}
                         </span>
								<p class="text-[9px] font-black text-gray-300 uppercase tracking-tighter truncate ml-2" title={dpi.matricola}>ID: {dpi.matricola}</p>
							</div>
							<h3 class="text-lg font-black text-[#1B4B6B] uppercase leading-tight mb-4 group-hover:text-blue-700 transition-colors">
								{dpi.nome}
							</h3>

							<div class="flex items-center gap-2 bg-gray-50 px-3 py-2 rounded-xl w-fit">
								<RefreshCw size={14} class="text-[#1B4B6B]" />
								<div class="flex flex-col">
									<span class="text-[7px] font-black text-gray-400 uppercase">Revisione</span>
									<span class="text-[10px] font-black text-[#1B4B6B]">{dpi.revisione}</span>
								</div>
							</div>
						</div>
					</div>
				</div>
			{/each}

			{#if dpiFiltrati.length === 0}
				<div class="col-span-1 md:col-span-2 py-20 bg-white border border-gray-100 rounded-[2.5rem] flex flex-col items-center justify-center text-center shadow-sm">
					<HardHat size={48} class="text-gray-200 mb-4" />
					<h3 class="text-xl font-black text-[#1B4B6B] uppercase italic">Nessun dispositivo trovato</h3>
					<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-2">L'inventario non contiene corrispondenze per i criteri inseriti.</p>
				</div>
			{/if}
		</div>
	{/if}
</div>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>