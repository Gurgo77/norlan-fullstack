<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale, slide } from 'svelte/transition';
	import {
		Search, Plus, ShieldCheck, AlertTriangle, Clock, Loader2, RefreshCw, X, Save, HardHat
	} from 'lucide-svelte';

	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { AuthService } from '$lib/services/AuthService';

	// --- INTERFACCE LOCALI ---
	interface DpiRegistro {
		idAssegnazione?: number;
		id?: number;
		idDipendente: number;
		nomeCompletoDipendente: string;
		statoDerivato: 'OK' | 'DA_REVISIONARE' | 'SCADUTO';
		tipo?: string;
		nomeDpi?: string;
		dataConsegna?: string;
		dataScadenzaRevisione?: string;
	}

	interface FormDPI {
		idAssegnazione: number | null;
		idDipendente: number | '';
		tipo: string;
		nomeDpi: string;
		dataConsegna: string;
		dataScadenzaRevisione: string;
	}

	// --- STATO REATTIVO (Svelte 5) ---
	let isLoading = $state(true);
	let searchQuery = $state('');
	let filtroAttivo = $state('TUTTI');
	let registro = $state<DpiRegistro[]>([]);
	let dipendentiList = $state<any[]>([]); // Per la tendina di selezione

	// Stati del Modale
	let showDpiModal = $state(false);
	let isSavingDpi = $state(false);
	let formDpi = $state<FormDPI>({
		idAssegnazione: null,
		idDipendente: '',
		tipo: '',
		nomeDpi: '',
		dataConsegna: '',
		dataScadenzaRevisione: ''
	});

	// --- LOGICA DI CARICAMENTO ---
	onMount(async () => {
		const session = AuthService.getSession();
		if (!session) return;

		try {
			const dipendenti = await LavoratoreService.getByAzienda(session.idUtente);
			dipendentiList = dipendenti;

			const promises = dipendenti.map(async (d) => {
				const dpiLavoratore = await LavoratoreService.getDpiByLavoratore(d.idUtente);

				return dpiLavoratore.map((dpi: any) => ({
					...dpi,
					idDipendente: d.idUtente,
					nomeCompletoDipendente: `${d.nome} ${d.cognome}`.toUpperCase(),
					statoDerivato: calcolaStato(dpi.dataScadenzaRevisione)
				})) as DpiRegistro[];
			});

			const risultati = await Promise.all(promises);
			registro = risultati.flat();

		} catch (error) {
			console.error("Errore nel caricamento del registro DPI:", error);
		} finally {
			isLoading = false;
		}
	});

	function calcolaStato(dataScadenzaStr: string | undefined): 'OK' | 'DA_REVISIONARE' | 'SCADUTO' {
		if (!dataScadenzaStr) return 'OK';
		const oggi = new Date();
		const scadenza = new Date(dataScadenzaStr);
		const diffGiorni = Math.ceil((scadenza.getTime() - oggi.getTime()) / (1000 * 3600 * 24));

		if (diffGiorni < 0) return 'SCADUTO';
		if (diffGiorni <= 30) return 'DA_REVISIONARE';
		return 'OK';
	}

	function formattaData(dateStr: string | undefined) {
		if (!dateStr) return 'N.D.';
		return new Date(dateStr).toLocaleDateString('it-IT');
	}

	// --- AZIONI MODALE E SALVATAGGIO ---
	function openNewDpiModal() {
		formDpi = { idAssegnazione: null, idDipendente: '', tipo: '', nomeDpi: '', dataConsegna: '', dataScadenzaRevisione: '' };
		showDpiModal = true;
	}

	function openUpdateDpiModal(dpi: DpiRegistro) {
		formDpi = {
			idAssegnazione: dpi.idAssegnazione || dpi.id || null,
			idDipendente: dpi.idDipendente,
			tipo: dpi.tipo || '',
			nomeDpi: dpi.nomeDpi || '',
			dataConsegna: '',
			dataScadenzaRevisione: ''
		};
		showDpiModal = true;
	}

	async function salvaDPI() {
		if (!formDpi.idDipendente) return;
		isSavingDpi = true;
		try {
			const payload = {
				idAssegnazione: formDpi.idAssegnazione,
				tipo: formDpi.tipo,
				nomeDpi: formDpi.tipo === 'ALTRO' ? formDpi.nomeDpi : undefined,
				dataConsegna: formDpi.dataConsegna ? formDpi.dataConsegna : undefined,
				dataScadenzaRevisione: formDpi.dataScadenzaRevisione
			};

			const savedDpi = await LavoratoreService.assegnaDpi(formDpi.idDipendente as number, payload as any);

			const dip = dipendentiList.find(d => d.idUtente === formDpi.idDipendente);
			const nomeCompleto = dip ? `${dip.nome} ${dip.cognome}`.toUpperCase() : 'SCONOSCIUTO';

			const nuovoRecord: DpiRegistro = {
				...(savedDpi as any),
				idDipendente: formDpi.idDipendente,
				nomeCompletoDipendente: nomeCompleto,
				statoDerivato: calcolaStato((savedDpi as any).dataScadenzaRevisione)
			};

			if (formDpi.idAssegnazione) {
				registro = registro.map(r => (r.idAssegnazione === formDpi.idAssegnazione || r.id === formDpi.idAssegnazione) ? nuovoRecord : r);
			} else {
				registro = [...registro, nuovoRecord];
			}

			showDpiModal = false;
		} catch (error) {
			alert("Errore salvataggio DPI.");
		} finally {
			isSavingDpi = false;
		}
	}

	// --- LOGICA REATTIVA CON ORDINAMENTO ---
	const filteredRegistro = $derived(
			registro.filter(d => {
				const tipoSafe = d.tipo ? d.tipo.toString().toLowerCase() : '';
				const nomeDpiSafe = d.nomeDpi ? d.nomeDpi.toLowerCase() : '';
				const matchSearch = d.nomeCompletoDipendente.toLowerCase().includes(searchQuery.toLowerCase()) ||
						tipoSafe.includes(searchQuery.toLowerCase()) ||
						nomeDpiSafe.includes(searchQuery.toLowerCase());
				const matchFiltro = filtroAttivo === 'TUTTI' || d.statoDerivato === filtroAttivo;
				return matchSearch && matchFiltro;
			}).sort((a, b) => {
				// Ordinamento di precedenza: SCADUTO (1), DA_REVISIONARE (2), OK (3)
				const priorita = { 'SCADUTO': 1, 'DA_REVISIONARE': 2, 'OK': 3 };
				if (priorita[a.statoDerivato] !== priorita[b.statoDerivato]) {
					return priorita[a.statoDerivato] - priorita[b.statoDerivato];
				}
				// A parità di stato, metti prima quelli con la data di scadenza più imminente
				const dataA = a.dataScadenzaRevisione ? new Date(a.dataScadenzaRevisione).getTime() : Infinity;
				const dataB = b.dataScadenzaRevisione ? new Date(b.dataScadenzaRevisione).getTime() : Infinity;
				return dataA - dataB;
			})
	);

	const stats = $derived({
		scaduti: registro.filter(d => d.statoDerivato === 'SCADUTO').length
	});
</script>

<div in:fade>
	<div class="mb-10 flex justify-between items-start">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Registro DPI</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Gestione assegnazione e ispezione attrezzature NorLan.</p>
		</div>

		<div class="flex items-center gap-6">
			<div class="bg-white p-4 rounded-2xl shadow-sm border border-red-50 flex items-center gap-4">
				<div class="bg-red-50 p-2 rounded-lg text-red-500"><AlertTriangle size={20} /></div>
				<div>
					<p class="text-[9px] font-bold text-gray-400 uppercase">Scaduti</p>
					<p class="text-xs font-black text-red-600 uppercase">{stats.scaduti}</p>
				</div>
			</div>

			<button
					onclick={openNewDpiModal}
					class="bg-white text-[#1B4B6B] border-2 border-[#1B4B6B] px-8 py-3.5 rounded-xl font-extrabold uppercase text-xs shadow-lg hover:bg-[#1B4B6B] hover:text-white transition-all flex items-center gap-3"
			>
				<Plus size={18} />
				Assegna Nuovo DPI
			</button>
		</div>
	</div>

	<div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 mb-8 flex flex-col md:flex-row justify-between items-center gap-6">
		<div class="flex gap-2">
			{#each ['TUTTI', 'OK', 'DA_REVISIONARE', 'SCADUTO'] as f (f)}
				<button
						onclick={() => (filtroAttivo = f)}
						class="px-4 py-2 rounded-xl text-[10px] font-black uppercase transition-all {filtroAttivo === f ? 'bg-[#1B4B6B] text-white' : 'bg-gray-50 text-gray-400 hover:bg-gray-100'}"
				>
					{f.replace('_', ' ')}
				</button>
			{/each}
		</div>

		<div class="relative w-full md:w-96">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
			<input
					bind:value={searchQuery}
					type="text"
					placeholder="Cerca dipendente o DPI..."
					class="w-full pl-12 pr-4 py-3 bg-gray-50 border-transparent rounded-2xl focus:ring-2 focus:ring-[#1B4B6B]/10 outline-none font-bold text-xs uppercase transition-all"
			/>
		</div>
	</div>

	<div class="bg-white rounded-3xl shadow-sm border border-gray-100 overflow-hidden">
		<div class="overflow-x-auto">
			<table class="w-full text-left">
				<thead class="bg-gray-50/50 text-[10px] font-bold text-gray-400 uppercase tracking-widest">
				<tr>
					<th class="px-8 py-5 text-center w-20">DPI</th>
					<th class="px-6 py-5 text-center">Dipendente</th>
					<th class="px-6 py-5 text-center">Tipo Dispositivo</th>
					<th class="px-6 py-5 text-center">Data Consegna</th>
					<th class="px-6 py-5 text-center">Revisione</th>
					<th class="px-6 py-5 text-center">Stato</th>
					<th class="px-6 py-5 text-right">Azioni</th>
				</tr>
				</thead>
				<tbody class="divide-y divide-gray-50">
				{#if isLoading}
					<tr><td colspan="7" class="px-8 py-20 text-center text-gray-300 font-black uppercase text-xs tracking-widest"><Loader2 size={32} class="animate-spin mx-auto mb-2" />Sincronizzazione...</td></tr>
				{:else}
					{#each filteredRegistro as item (item.idAssegnazione || item.id)}
						{@const nomeDpiReale = (item.tipo === 'ALTRO' && item.nomeDpi) ? item.nomeDpi : (item.tipo || 'NON DEFINITO').replace(/_/g, ' ')}

						<tr class="hover:bg-white hover:shadow-xl hover:shadow-blue-900/5 transition-all group relative">
							<td class="px-8 py-4 text-center">
								<!-- Icona HardHat Semaforo -->
								<div class="h-12 w-12 mx-auto rounded-xl inline-flex items-center justify-center font-black transition-colors {
                             item.statoDerivato === 'OK' ? 'bg-green-50 text-green-600 group-hover:bg-green-600 group-hover:text-white' :
                             item.statoDerivato === 'DA_REVISIONARE' ? 'bg-yellow-50 text-yellow-600 group-hover:bg-yellow-500 group-hover:text-white' :
                             'bg-red-50 text-red-600 group-hover:bg-red-600 group-hover:text-white'
                         }">
									<HardHat size={24} />
								</div>
							</td>
							<td class="px-6 py-6 text-center"><span class="font-black text-[#1B4B6B] text-xs uppercase">{item.nomeCompletoDipendente}</span></td>
							<td class="px-6 py-6 text-center">
								<span class="font-bold text-[#1B4B6B] text-xs uppercase">{nomeDpiReale}</span>
							</td>
							<td class="px-6 py-6 text-xs text-gray-400 font-medium text-center">{formattaData(item.dataConsegna)}</td>
							<td class="px-6 py-6 text-xs font-black text-[#1B4B6B] text-center">{formattaData(item.dataScadenzaRevisione)}</td>
							<td class="px-6 py-6 text-center">
								<!-- Badge Stato -->
								<div class="inline-flex items-center gap-2 px-3 py-1 rounded-full border text-[9px] font-black uppercase {item.statoDerivato === 'OK' ? 'bg-green-50 text-green-600 border-green-100' : item.statoDerivato === 'DA_REVISIONARE' ? 'bg-yellow-50 text-yellow-600 border-yellow-100' : 'bg-red-50 text-red-600 border-red-100'}">
									{#if item.statoDerivato === 'OK'}<ShieldCheck size={12} />{:else if item.statoDerivato === 'DA_REVISIONARE'}<Clock size={12} />{:else}<AlertTriangle size={12} />{/if}
									{item.statoDerivato.replace('_', ' ')}
								</div>
							</td>
							<td class="px-6 py-6 text-right">
								<button onclick={() => openUpdateDpiModal(item)} class="inline-flex items-center gap-2 px-4 py-2 bg-gray-50 text-[#1B4B6B] border border-transparent rounded-xl text-[9px] font-black uppercase tracking-widest hover:border-[#1B4B6B] hover:bg-white transition-all opacity-0 group-hover:opacity-100">
									<RefreshCw size={14} /> Aggiorna
								</button>
							</td>
						</tr>
					{/each}
					{#if filteredRegistro.length === 0}
						<tr><td colspan="7" class="px-8 py-10 text-center text-gray-400 font-bold uppercase text-xs">Nessun DPI trovato nei registri.</td></tr>
					{/if}
				{/if}
				</tbody>
			</table>
		</div>
	</div>
</div>

<!-- MODALE INSERIMENTO / AGGIORNAMENTO DPI -->
{#if showDpiModal}
	<div class="fixed inset-0 bg-[#1B4B6B]/40 backdrop-blur-sm flex items-center justify-center z-[110] p-4" transition:fade>
		<div class="bg-white rounded-3xl shadow-2xl w-full max-w-lg overflow-hidden" in:scale>
			<div class="bg-[#1B4B6B] p-6 text-white flex justify-between items-center">
				<h2 class="text-xl font-black uppercase tracking-tighter flex items-center gap-2">
					<ShieldCheck size={20}/> {formDpi.idAssegnazione ? 'Aggiorna DPI Lavoratore' : 'Registra Consegna DPI'}
				</h2>
				<button onclick={() => (showDpiModal = false)} class="hover:rotate-90 transition-transform"><X size={24}/></button>
			</div>
			<div class="p-8 space-y-6">

				<div>
					<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Seleziona Dipendente *</label>
					<select bind:value={formDpi.idDipendente} disabled={!!formDpi.idAssegnazione} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold uppercase focus:ring-2 focus:ring-[#1B4B6B] outline-none disabled:opacity-50">
						<option value="" disabled>-- Scegli dipendente --</option>
						{#each dipendentiList as dip}
							<option value={dip.idUtente}>{dip.nome} {dip.cognome}</option>
						{/each}
					</select>
				</div>

				<div>
					<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Tipologia DPI *</label>
					<select bind:value={formDpi.tipo} disabled={!!formDpi.idAssegnazione} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold uppercase focus:ring-2 focus:ring-[#1B4B6B] outline-none disabled:opacity-50">
						<option value="">Seleziona DPI...</option>
						<option value="ELMETTO">Elmetto</option>
						<option value="GUANTI">Guanti</option>
						<option value="SCARPE_ANTINFORTUNISTICHE">Scarpe Antinfortunistiche</option>
						<option value="OCCHIALI">Occhiali</option>
						<option value="ALTRO">Altro</option>
					</select>
				</div>

				{#if formDpi.tipo === 'ALTRO'}
					<div class="space-y-1" transition:slide>
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase">Nome DPI Personalizzato *</label>
						<input bind:value={formDpi.nomeDpi} disabled={!!formDpi.idAssegnazione} type="text" placeholder="Specifica il nome del DPI..." class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-[#1B4B6B] outline-none disabled:opacity-50" />
					</div>
				{/if}

				<div class="grid grid-cols-2 gap-4 mt-4">
					<div>
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Nuova Data Consegna *</label>
						<input type="date" max="9999-12-31" bind:value={formDpi.dataConsegna} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
					</div>
					<div>
						<label class="block text-[10px] font-bold text-[#1B4B6B] uppercase mb-1">Scadenza Revisione *</label>
						<input type="date" max="9999-12-31" bind:value={formDpi.dataScadenzaRevisione} class="w-full p-3 bg-gray-50 border-none rounded-xl text-sm font-bold focus:ring-2 focus:ring-[#1B4B6B] outline-none" />
					</div>
				</div>
			</div>
			<div class="p-8 bg-gray-50 flex justify-end gap-4 border-t border-gray-100">
				<button onclick={() => (showDpiModal = false)} class="px-6 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">Annulla</button>
				<button
						onclick={salvaDPI}
						disabled={isSavingDpi || !formDpi.idDipendente || !formDpi.tipo || (formDpi.tipo === 'ALTRO' && !formDpi.nomeDpi.trim()) || !formDpi.dataConsegna || !formDpi.dataScadenzaRevisione}
						class="bg-[#1B4B6B] text-white px-8 py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center gap-2 hover:bg-[#1B4B6B]/90 transition-colors"
				>
					{#if isSavingDpi}<Loader2 size={14} class="animate-spin" />{:else}<Save size={14} />{/if} {isSavingDpi ? 'Salvataggio...' : 'Conferma'}
				</button>
			</div>
		</div>
	</div>
{/if}

<style>
	:global(body) { background-color: #F9FAFB; }
</style>