<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		HardHat, ShieldCheck, ShieldAlert, ShieldOff,
		Search, Loader2, Info, RefreshCw, FileText
	} from 'lucide-svelte';

	import { Dipendente } from '$lib/models/Dipendente';
	import { AuthService } from '$lib/services/AuthService';

	// 1. DEFINIZIONE TIPI (Interna per evitare l'errore "Module not found")
	type ComplianceStatus = 'OK' | 'WARNING' | 'DANGER';
	type OpzioneFiltro = 'TUTTI' | ComplianceStatus;

	interface Dpi {
		id: number;
		nome: string;
		matricola: string;
		stato: ComplianceStatus;
		revisione: string;
	}

	// 2. STATO CON RUNE
	let isLoading = $state(true);
	let searchQuery = $state('');
	let filtroStato = $state<OpzioneFiltro>('TUTTI');
	let dotazioni = $state<Dpi[]>([]);
	let utente = $state<Dipendente | null>(null);

	// Array di opzioni per il filtro (definito qui per evitare errori nel template)
	const opzioniFiltro: OpzioneFiltro[] = ['TUTTI', 'OK', 'WARNING', 'DANGER'];

	// 3. HELPER (Risolve l'errore "Unexpected any" e "Unexpected token")
	function impostaFiltro(valore: OpzioneFiltro) {
		filtroStato = valore;
	}

	function getStatusConfig(stato: ComplianceStatus) {
		const configs = {
			OK: { color: 'text-emerald-500', bg: 'bg-emerald-50', icon: ShieldCheck, label: 'REGOLARE' },
			WARNING: { color: 'text-amber-500', bg: 'bg-amber-50', icon: ShieldAlert, label: 'IN SCADENZA' },
			DANGER: { color: 'text-red-500', bg: 'bg-red-50', icon: ShieldOff, label: 'NON CONFORME' }
		};
		return configs[stato];
	}

	onMount(() => {
		setTimeout(() => {
			utente = AuthService.getSession();
			dotazioni = [
				{ id: 1, nome: 'ELMETTO PROTETTIVO ALTA VISIBILITÀ', matricola: 'NOR-E-2024-001', stato: 'OK', revisione: '15/01/2028' },
				{ id: 2, nome: 'IMBRACATURA ANTICADUTA PRO-BELT', matricola: 'NOR-I-2023-442', stato: 'DANGER', revisione: 'SCADUTA' },
				{ id: 3, nome: 'OCCHIALI ANTIAPPANNANTI', matricola: 'NOR-O-2025-112', stato: 'OK', revisione: 'NON PREVISTA' },
				{ id: 4, nome: 'GUANTI ANTITAGLIO LIVELLO 5', matricola: 'NOR-G-2026-990', stato: 'WARNING', revisione: 'TRA 10 GG' }
			];
			isLoading = false;
		}, 600);
	});

	// 4. LOGICA REATTIVA
	const dpiFiltrati = $derived(
		dotazioni.filter(d => {
			const matchSearch = d.nome.toLowerCase().includes(searchQuery.toLowerCase()) ||
				d.matricola.toLowerCase().includes(searchQuery.toLowerCase());
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
				Utente: <span class="text-[#1B4B6B]">{utente?.nome ?? 'Dipendente'}</span> | Gestione dotazioni e scadenze
			</p>
		</div>

		<div class="flex gap-4">
			<div class="bg-white px-6 py-4 rounded-2xl border border-gray-100 shadow-sm flex items-center gap-4">
				<div class="w-10 h-10 bg-red-50 text-red-500 rounded-xl flex items-center justify-center font-black">{conteggi.critici}</div>
				<span class="text-[9px] font-black text-gray-400 uppercase tracking-widest leading-tight">Critici</span>
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

		<div class="flex bg-gray-100 p-1.5 rounded-[1.5rem] gap-1">
			{#each opzioniFiltro as opzione (opzione)}
				<button
					onclick={() => impostaFiltro(opzione)}
					class="px-5 py-2.5 rounded-xl text-[9px] font-black uppercase tracking-widest transition-all
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
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Sincronizzazione inventario...</span>
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
								<p class="text-[9px] font-black text-gray-300 uppercase tracking-tighter">ID: {dpi.matricola}</p>
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

					<div class="p-8 pt-0 mt-auto flex gap-3">
						<button class="flex-1 bg-gray-50 text-[#1B4B6B] py-4 rounded-2xl text-[10px] font-black uppercase flex items-center justify-center gap-2 border border-gray-100">
							<FileText size={18} /> Scheda Tecnica
						</button>
						<button class="flex-1 bg-white text-[#1B4B6B] py-4 rounded-2xl text-[10px] font-black uppercase flex items-center justify-center gap-2 border border-gray-100 shadow-sm">
							<Info size={18} /> Dettagli
						</button>
					</div>
				</div>
			{/each}
		</div>
	{/if}
</div>

<style>
    :global(body) { background-color: #F9FAFB; }
</style>