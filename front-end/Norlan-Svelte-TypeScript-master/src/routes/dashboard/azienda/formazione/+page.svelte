<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Search, Filter, User, ShieldCheck, AlertTriangle,
		ArrowRight, Bookmark, Loader2, GraduationCap
	} from 'lucide-svelte';

	// --- INTERFACCE TIPIZZATE ---
	interface CorsoStato {
		nome: string;
		scadenza: string;
		stato: 'OK' | 'SCADENZA' | 'CRITICO';
	}

	interface DipendenteFormazione {
		id: number;
		nome: string;
		ruolo: string;
		corsi: CorsoStato[];
	}

	let isLoading = $state(true);
	let searchQuery = $state('');

	let dipendenti = $state<DipendenteFormazione[]>([
		{
			id: 1,
			nome: 'MARIO ROSSI',
			ruolo: 'OPERAIO SPECIALIZZATO',
			corsi: [
				{ nome: 'SICUREZZA GENERALE', scadenza: '10/05/2028', stato: 'OK' },
				{ nome: 'ANTINCENDIO RISCHIO MEDIO', scadenza: '15/06/2026', stato: 'OK' },
				{ nome: 'PRIMO SOCCORSO', scadenza: '01/03/2026', stato: 'SCADENZA' }
			]
		},
		{
			id: 2,
			nome: 'LUIGI BIANCHI',
			ruolo: 'MAGAZZINIERE',
			corsi: [
				{ nome: 'SICUREZZA GENERALE', scadenza: '12/02/2028', stato: 'OK' },
				{ nome: 'CARRELISTI (MULETTO)', scadenza: '10/01/2025', stato: 'CRITICO' }
			]
		},
		{
			id: 3,
			nome: 'ANNA VERDI',
			ruolo: 'IMPIEGATA AMMINISTRATIVA',
			corsi: [
				{ nome: 'SICUREZZA GENERALE', scadenza: '20/11/2029', stato: 'OK' }
			]
		}
	]);

	onMount(() => {
		setTimeout(() => {
			isLoading = false;
		}, 500);
	});

	const filteredDipendenti = $derived(
		dipendenti.filter(d => d.nome.toLowerCase().includes(searchQuery.toLowerCase()))
	);
</script>

<div in:fade>
	<div class="mb-10 flex justify-between items-start">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Formazione Dipendenti</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Monitoraggio attestati e scadenze corsi.</p>
		</div>

		<div class="bg-white p-4 rounded-2xl shadow-sm border border-red-50 flex items-center gap-4">
			<div class="bg-red-50 p-2 rounded-lg text-red-500">
				<AlertTriangle size={20} />
			</div>
			<div>
				<p class="text-[9px] font-bold text-gray-400 uppercase">Da Aggiornare</p>
				<p class="text-xs font-black text-red-600 uppercase">2 Dipendenti</p>
			</div>
		</div>
	</div>

	<div class="mb-10 flex gap-4">
		<div class="relative flex-1 group">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={18} />
			<input
				bind:value={searchQuery}
				type="text"
				placeholder="Cerca dipendente per nome..."
				class="w-full pl-12 pr-4 py-4 bg-white border border-gray-100 rounded-2xl text-xs font-bold uppercase outline-none focus:ring-4 focus:ring-[#1B4B6B]/5 transition-all"
			/>
		</div>
		<button class="p-4 bg-white border border-gray-100 rounded-2xl text-gray-400 hover:text-[#1B4B6B] transition-all shadow-sm">
			<Filter size={20} />
		</button>
	</div>

	{#if isLoading}
		<div class="py-20 flex flex-col items-center justify-center gap-4">
			<Loader2 size={40} class="animate-spin text-[#1B4B6B]" />
			<p class="text-[10px] font-black text-gray-300 uppercase tracking-widest">Sincronizzazione registri...</p>
		</div>
	{:else}
		<div class="space-y-6">
			{#each filteredDipendenti as dip (dip.id)}
				<div
					class="bg-white p-6 rounded-[32px] border border-gray-100 shadow-sm flex items-center gap-8 hover:shadow-xl hover:border-[#1B4B6B]/20 transition-all duration-300 group cursor-default"
					in:scale
				>
					<div class="size-16 shrink-0 flex-none bg-[#1B4B6B] rounded-2xl flex items-center justify-center text-white shadow-lg shadow-[#1B4B6B]/20">
						<User size={28} />
					</div>

					<div class="w-64 shrink-0">
						<h3 class="font-black text-[#1B4B6B] text-lg uppercase leading-tight">{dip.nome}</h3>
						<p class="text-[10px] font-bold text-gray-400 uppercase tracking-tight">{dip.ruolo}</p>
					</div>

					<div class="flex-1 flex flex-wrap gap-3">
						{#each dip.corsi as corso}
							<div class="px-4 py-2 rounded-xl border flex items-center gap-3 {corso.stato === 'OK' ? 'bg-green-50 border-green-100 text-green-600' : corso.stato === 'SCADENZA' ? 'bg-yellow-50 border-yellow-100 text-yellow-600' : 'bg-red-50 border-red-100 text-red-600'}">
								<div>
									<p class="text-[9px] font-black uppercase tracking-tighter leading-none">{corso.nome}</p>
									<p class="text-[10px] font-bold mt-1 opacity-80">{corso.scadenza}</p>
								</div>
								{#if corso.stato === 'OK'}
									<ShieldCheck size={14} />
								{:else}
									<AlertTriangle size={14} />
								{/if}
							</div>
						{/each}
					</div>

					<div class="flex items-center gap-4">
						<button class="p-3 text-gray-300 hover:text-[#1B4B6B] transition-colors">
							<Bookmark size={20} />
						</button>

						<button class="flex items-center gap-3 bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-6 py-3 rounded-2xl font-black text-[11px] uppercase tracking-widest hover:bg-[#1B4B6B] hover:text-white transition-all shadow-sm">
							Gestisci
							<ArrowRight size={16} />
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