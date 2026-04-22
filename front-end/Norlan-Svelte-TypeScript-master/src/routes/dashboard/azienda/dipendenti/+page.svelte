<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import {
		Users, UserPlus, Search,
		MoreHorizontal, Mail, Phone, Briefcase,
		ShieldCheck, ShieldAlert, Loader2, Eye
	} from 'lucide-svelte';

	interface Dipendente {
		id: number;
		nome: string;
		email: string;
		telefono: string;
		ruolo: string;
		dataAssunzione: string;
		stato: 'ATTIVO' | 'SOSPESO';
	}

	let isLoading = $state(true);
	let searchQuery = $state('');
	let dipendenti = $state<Dipendente[]>([]);

	onMount(() => {
		setTimeout(() => {
			dipendenti = [
				{ id: 1, nome: 'Mario Rossi', email: 'm.rossi@ede.it', telefono: '333 1234567', ruolo: 'Operaio Specializzato', dataAssunzione: '10/01/2020', stato: 'ATTIVO' },
				{ id: 2, nome: 'Luigi Bianchi', email: 'l.bianchi@ede.it', telefono: '333 7654321', ruolo: 'Magazziniere', dataAssunzione: '15/03/2022', stato: 'ATTIVO' },
				{ id: 3, nome: 'Anna Verdi', email: 'a.verdi@ede.it', telefono: '340 1122334', ruolo: 'Amministrazione', dataAssunzione: '01/06/2021', stato: 'ATTIVO' },
				{ id: 4, nome: 'Francesco Neri', email: 'f.neri@ede.it', telefono: '339 9988776', ruolo: 'Manutentore', dataAssunzione: '20/11/2023', stato: 'SOSPESO' }
			];
			isLoading = false;
		}, 600);
	});

	const filteredDipendenti = $derived(
		dipendenti.filter(d => d.nome.toLowerCase().includes(searchQuery.toLowerCase()))
	);
</script>

<div in:fade>
	<div class="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 mb-10">
		<div>
			<h1 class="text-4xl font-black text-[#1B4B6B] uppercase tracking-tighter">Anagrafica Dipendenti</h1>
			<p class="text-xs font-bold text-gray-400 uppercase tracking-widest mt-1">Gestione dei lavoratori e delle posizioni aziendali.</p>
		</div>

		<button class="flex items-center justify-center gap-2 bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-6 py-4 rounded-2xl font-black text-xs uppercase tracking-widest hover:bg-[#1B4B6B] hover:text-white transition-all shadow-sm">
			<UserPlus size={18} />
			Aggiungi Dipendente
		</button>
	</div>

	<div class="bg-white p-4 rounded-2xl shadow-sm border border-gray-100 mb-8 group">
		<div class="relative w-full">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-[#1B4B6B] transition-colors" size={18} />
			<input
				bind:value={searchQuery}
				type="text"
				placeholder="Cerca dipendente..."
				class="w-full pl-12 pr-4 py-3 bg-gray-50 border-transparent rounded-xl focus:ring-2 focus:ring-[#1B4B6B]/10 focus:border-[#1B4B6B] outline-none font-bold text-sm transition-all"
			/>
		</div>
	</div>

	{#if isLoading}
		<div class="py-20 flex flex-col items-center justify-center gap-4 text-gray-300">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<p class="text-[10px] font-black uppercase tracking-widest">Caricamento...</p>
		</div>
	{:else}
		<div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-8">
			{#each filteredDipendenti as dip (dip.id)}
				<div class="bg-white rounded-[40px] shadow-sm border border-gray-100 p-8 hover:shadow-2xl hover:border-[#1B4B6B]/30 hover:-translate-y-1 transition-all duration-300 group relative overflow-hidden" in:scale>
					<div class="absolute top-6 right-6">
       <span class="text-[8px] font-black px-2 py-1 rounded-full border uppercase {dip.stato === 'ATTIVO' ? 'bg-green-50 text-green-600 border-green-100' : 'bg-red-50 text-red-600 border-red-100'}">
        {dip.stato}
       </span>
					</div>

					<div class="flex items-center gap-4 mb-8">
						<div class="size-16 shrink-0 flex-none bg-[#1B4B6B] rounded-[22px] flex items-center justify-center text-white shadow-lg shadow-[#1B4B6B]/20 transition-all group-hover:scale-105">
							<span class="text-xl font-black">{dip.nome.split(' ').map(n => n[0]).join('')}</span>
						</div>
						<div class="min-w-0"> <h3 class="font-black text-[#1B4B6B] text-lg uppercase leading-tight truncate">{dip.nome}</h3>
							<div class="flex items-center gap-1.5 text-gray-400">
								<Briefcase size={12} />
								<p class="text-[10px] font-bold uppercase tracking-tight truncate">{dip.ruolo}</p>
							</div>
						</div>
					</div>

					<div class="space-y-3 mb-8">
						<div class="flex items-center gap-3 text-xs font-medium text-gray-500">
							<Mail size={14} class="text-[#1B4B6B] shrink-0" />
							<span class="truncate">{dip.email}</span>
						</div>
						<div class="flex items-center gap-3 text-xs font-medium text-gray-500">
							<Phone size={14} class="text-[#1B4B6B] shrink-0" />
							<span>{dip.telefono}</span>
						</div>
					</div>

					<div class="flex items-center justify-between pt-6 border-t border-gray-50">
						<div class="flex gap-1 text-gray-300">
							<ShieldCheck size={16} class={dip.stato === 'ATTIVO' ? 'text-green-500' : ''} />
							<ShieldAlert size={16} class={dip.stato === 'SOSPESO' ? 'text-red-500' : ''} />
						</div>

						<div class="flex gap-2">
							<button class="p-2.5 bg-gray-50 text-gray-400 hover:text-[#1B4B6B] hover:bg-blue-50 rounded-xl transition-all">
								<MoreHorizontal size={20} />
							</button>
							<a href="/dashboard/azienda/formazione" class="flex items-center gap-2 bg-[#1B4B6B] text-white px-4 py-2.5 rounded-xl font-black text-[9px] uppercase tracking-widest hover:bg-[#153a54] transition-all">
								<Eye size={14} /> Profilo
							</a>
						</div>
					</div>
				</div>
			{/each}
		</div>
	{/if}

	{#if !isLoading && filteredDipendenti.length === 0}
		<div class="bg-white border-2 border-dashed border-gray-100 rounded-[40px] p-20 text-center">
			<Users size={48} class="mx-auto text-gray-200 mb-4" />
			<h3 class="text-[#1B4B6B] font-black uppercase text-xl">Nessun risultato</h3>
		</div>
	{/if}
</div>

<style>
    :global(body) { background-color: #F9FAFB; }
</style>