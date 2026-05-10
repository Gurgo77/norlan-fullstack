<script lang="ts">
	import { onMount } from 'svelte';
	import { fade, scale, slide } from 'svelte/transition';
	import {
		Search, Plus, ShieldCheck, AlertTriangle, Loader2, Save, HardHat, User, AlertCircle
	} from 'lucide-svelte';

	import { LavoratoreService } from '$lib/services/LavoratoreService';
	import { AuthService } from '$lib/services/AuthService';
	import StatCard from '$lib/Components/UI/StatCard.svelte';
	import DpiCard from '$lib/Components/Features/Documentale/DpiCard.svelte';
	import ModalCard from '$lib/Components/UI/ModalCard.svelte';
	import { getInfoScadenza, formattaDataScadenza } from '$lib/utils/scadenzeUtils';

	interface DpiRegistro {
		idAssegnazione?: number;
		id?: number;
		idDipendente: number;
		nomeCompletoDipendente: string;
		statoDerivato: 'OK' | 'WARNING' | 'DANGER';
		tipo?: string;
		nomeDpi?: string;
		dataConsegna?: string;
		dataScadenzaRevisione?: string;
		matricola?: string;
		note?: string;
	}

	interface FormDPI {
		idAssegnazione: number | null;
		idDipendente: number | '';
		tipo: string;
		nomeDpi: string;
		dataConsegna: string;
		dataScadenzaRevisione: string;
	}

	let isLoading = $state(true);
	let searchQuery = $state('');
	let filtroAttivo = $state('TUTTI');
	let registro = $state<DpiRegistro[]>([]);
	let dipendentiList = $state<any[]>([]);

	let showDpiModal = $state(false);
	let isSavingDpi = $state(false);
	let formDpi = $state<FormDPI>({
		idAssegnazione: null, idDipendente: '', tipo: '', nomeDpi: '', dataConsegna: '', dataScadenzaRevisione: ''
	});

	let showErrorModal = $state(false);
	let errorMessage = $state('');
	let showConfirmModal = $state(false);
	let dpiToDelete = $state<number | string | null>(null);

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
					statoDerivato: getInfoScadenza(dpi.dataScadenzaRevisione).stato,
					matricola: dpi.note || `ID-${dpi.idAssegnazione || dpi.id || 0}`
				})) as DpiRegistro[];
			});

			const risultati = await Promise.all(promises);
			registro = risultati.flat();
		} catch (error) {
			console.error("Errore caricamento registro DPI aziendale:", error);
		} finally {
			isLoading = false;
		}
	});

	function mostraErrore(messaggio: string) {
		errorMessage = messaggio;
		showErrorModal = true;
	}

	function richiediEliminazione(idDpi: number | string) {
		dpiToDelete = idDpi;
		showConfirmModal = true;
	}

	async function confermaEliminazione() {
		if (!dpiToDelete) return;
		try {
			await LavoratoreService.deleteDpi(Number(dpiToDelete));
			registro = registro.filter(d => d.idAssegnazione !== dpiToDelete && d.id !== dpiToDelete);
			showConfirmModal = false;
			dpiToDelete = null;
		} catch (error) {
			console.error(error);
			showConfirmModal = false;
			mostraErrore("Impossibile eliminare il DPI. Riprovare o contattare l'assistenza.");
		}
	}

	function openNewDpiModal() {
		formDpi = { idAssegnazione: null, idDipendente: '', tipo: '', nomeDpi: '', dataConsegna: '', dataScadenzaRevisione: '' };
		showDpiModal = true;
	}

	function openUpdateDpiModal(idDpi: number | string) {
		const dpi = registro.find(d => d.idAssegnazione === idDpi || d.id === idDpi);
		if (!dpi) return;
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
				statoDerivato: getInfoScadenza((savedDpi as any).dataScadenzaRevisione).stato,
				matricola: (savedDpi as any).note || `ID-${(savedDpi as any).idAssegnazione || (savedDpi as any).id || 0}`
			};

			if (formDpi.idAssegnazione) {
				registro = registro.map(r => (r.idAssegnazione === formDpi.idAssegnazione || r.id === formDpi.idAssegnazione) ? nuovoRecord : r);
			} else {
				registro = [...registro, nuovoRecord];
			}

			showDpiModal = false;
		} catch {
			mostraErrore("Impossibile completare l'operazione di salvataggio del dispositivo. Riprovare o contattare il supporto tecnico.");
		} finally {
			isSavingDpi = false;
		}
	}

	const filteredRegistro = $derived(
			registro.filter(d => {
				const tipoSafe = d.tipo ? d.tipo.toString().toLowerCase() : '';
				const nomeDpiSafe = d.nomeDpi ? d.nomeDpi.toLowerCase() : '';
				const matricolaSafe = d.matricola ? d.matricola.toLowerCase() : '';
				const matchSearch = d.nomeCompletoDipendente.toLowerCase().includes(searchQuery.toLowerCase()) ||
						tipoSafe.includes(searchQuery.toLowerCase()) ||
						matricolaSafe.includes(searchQuery.toLowerCase()) ||
						nomeDpiSafe.includes(searchQuery.toLowerCase());

				const matchFiltro = filtroAttivo === 'TUTTI' || d.statoDerivato === filtroAttivo;
				return matchSearch && matchFiltro;
			}).sort((a, b) => {
				const infoA = getInfoScadenza(a.dataScadenzaRevisione);
				const infoB = getInfoScadenza(b.dataScadenzaRevisione);

				if (infoA.peso !== infoB.peso) {
					return infoA.peso - infoB.peso;
				}
				const dataA = a.dataScadenzaRevisione ? new Date(a.dataScadenzaRevisione).getTime() : Infinity;
				const dataB = b.dataScadenzaRevisione ? new Date(b.dataScadenzaRevisione).getTime() : Infinity;
				return dataA - dataB;
			})
	);

	const groupedRegistro = $derived(
			Object.values(
					filteredRegistro.reduce((acc, dpi) => {
						if (!acc[dpi.idDipendente]) {
							acc[dpi.idDipendente] = { id: dpi.idDipendente, nome: dpi.nomeCompletoDipendente, dpis: [] };
						}
						acc[dpi.idDipendente].dpis.push(dpi);
						return acc;
					}, {} as Record<number, { id: number, nome: string, dpis: DpiRegistro[] }>)
			).sort((a, b) => a.nome.localeCompare(b.nome))
	);

	const stats = $derived({
		scaduti: registro.filter(d => getInfoScadenza(d.dataScadenzaRevisione).stato === 'DANGER').length,
		inScadenza: registro.filter(d => getInfoScadenza(d.dataScadenzaRevisione).stato === 'WARNING').length
	});
</script>

<div in:fade class="max-w-[1600px] mx-auto space-y-8 pb-10">
	<div class="mb-10 flex justify-between items-start">
		<div>
			<h1 class="text-4xl font-extrabold text-[#1B4B6B] uppercase tracking-tighter">Registro DPI</h1>
			<p class="text-gray-500 font-bold uppercase text-xs tracking-tighter">Gestione assegnazione e ispezione attrezzature NorLan.</p>
		</div>
		<div class="flex items-center gap-6">
			<div class="flex gap-4">
				<StatCard
						titolo="In Scadenza"
						valore={stats.inScadenza}
						icona={AlertTriangle}
						bgIcona="bg-amber-50"
						testoIcona="text-amber-500"
				/>
				<StatCard
						titolo="Scaduti"
						valore={stats.scaduti}
						icona={AlertTriangle}
						bgIcona="bg-red-50"
						testoIcona="text-red-500"
				/>
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
			{#each ['TUTTI', 'OK', 'WARNING', 'DANGER'] as f (f)}
				<button
						onclick={() => (filtroAttivo = f)}
						class="px-4 py-2 rounded-xl text-[10px] font-black uppercase transition-all {filtroAttivo === f ? 'bg-[#1B4B6B] text-white shadow-md' : 'bg-gray-50 text-gray-400 hover:bg-gray-100'}"
				>
					{f === 'WARNING' ? 'IN SCADENZA' : f === 'DANGER' ? 'SCADUTO' : f}
				</button>
			{/each}
		</div>
		<div class="relative w-full md:w-96">
			<Search class="absolute left-4 top-1/2 -translate-y-1/2 text-gray-400" size={18} />
			<input
					bind:value={searchQuery}
					type="text"
					placeholder="Cerca dipendente o matricola..."
					class="w-full pl-12 pr-4 py-3 bg-gray-50 border-transparent rounded-2xl focus:ring-2 focus:ring-[#1B4B6B]/10 outline-none font-bold text-xs uppercase transition-all"
			/>
		</div>
	</div>

	{#if isLoading}
		<div class="py-32 flex flex-col items-center justify-center gap-4">
			<Loader2 size={48} class="animate-spin text-[#1B4B6B]" />
			<span class="text-[10px] font-black text-gray-400 uppercase tracking-widest">Sincronizzazione registro DPI...</span>
		</div>
	{:else}
		<div class="space-y-12">
			{#each groupedRegistro as gruppo (gruppo.id)}
				<div in:fade>
					<div class="flex items-center gap-4 mb-6 border-b border-gray-100 pb-4">
						<div class="bg-[#1B4B6B] p-2.5 rounded-xl text-white shadow-sm">
							<User size={20} />
						</div>
						<h2 class="text-2xl font-black text-[#1B4B6B] uppercase tracking-tighter">{gruppo.nome}</h2>
						<span class="ml-auto bg-gray-50 text-[#1B4B6B] text-[10px] font-black px-4 py-2 rounded-full border border-gray-200 uppercase">
                      {gruppo.dpis.length} Dispositivi
                   </span>
					</div>

					<div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
						{#each gruppo.dpis as item (item.idAssegnazione || item.id)}
							{@const nomeDpiReale = (item.tipo === 'ALTRO' && item.nomeDpi) ? item.nomeDpi : (item.tipo || 'DPI').replace(/_/g, ' ')}
							<div in:scale={{duration: 300}}>
								<DpiCard
										ruolo="azienda"
										dpi={{
                               id: item.idAssegnazione || item.id || 0,
                               nome: nomeDpiReale,
                               matricola: item.matricola,
                               stato: item.statoDerivato,
                               dataRevisione: formattaDataScadenza(item.dataScadenzaRevisione),
                               dataConsegna: formattaDataScadenza(item.dataConsegna)
                            }}
										onModifica={openUpdateDpiModal}
										onElimina={richiediEliminazione}
								/>
							</div>
						{/each}
					</div>
				</div>
			{/each}
		</div>

		{#if filteredRegistro.length === 0}
			<div class="py-20 bg-white border border-gray-100 rounded-[2.5rem] flex flex-col items-center justify-center text-center shadow-sm">
				<HardHat size={48} class="text-gray-200 mb-4" />
				<h3 class="text-xl font-black text-[#1B4B6B] uppercase italic">Nessun DPI trovato</h3>
				<p class="text-[10px] font-bold text-gray-400 uppercase tracking-widest mt-2">Nessuna corrispondenza nei registri per i criteri selezionati.</p>
			</div>
		{/if}
	{/if}
</div>

<ModalCard bind:isOpen={showDpiModal} maxWidth="max-w-lg">
	{#snippet title()}
		<ShieldCheck size={20}/> <span class="font-black uppercase tracking-tighter">{formDpi.idAssegnazione ? 'Aggiorna DPI Lavoratore' : 'Registra Consegna DPI'}</span>
	{/snippet}

	<div class="space-y-6">
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

	{#snippet footer()}
		<button onclick={() => (showDpiModal = false)} class="flex-1 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">
			Annulla
		</button>
		<button
				onclick={salvaDPI}
				disabled={isSavingDpi || !formDpi.idDipendente || !formDpi.tipo || (formDpi.tipo === 'ALTRO' && !formDpi.nomeDpi.trim()) || !formDpi.dataConsegna || !formDpi.dataScadenzaRevisione}
				class="flex-1 bg-[#1B4B6B] text-white py-3 rounded-xl text-[10px] font-black uppercase shadow-lg disabled:opacity-50 flex items-center justify-center gap-2 hover:bg-[#1B4B6B]/90 transition-colors"
		>
			{#if isSavingDpi}<Loader2 size={14} class="animate-spin" />{:else}<Save size={14} />{/if} {isSavingDpi ? 'Salvataggio...' : 'Conferma'}
		</button>
	{/snippet}
</ModalCard>

<ModalCard bind:isOpen={showConfirmModal} headerClass="bg-red-500" maxWidth="max-w-md">
	{#snippet title()}
		<AlertTriangle size={20} /> <span class="font-black uppercase tracking-tighter">Attenzione</span>
	{/snippet}

	<div class="text-center py-4">
		<div class="w-16 h-16 bg-red-50 text-red-500 rounded-full flex items-center justify-center mx-auto mb-4">
			<AlertTriangle size={32} />
		</div>
		<p class="text-sm font-bold text-gray-600 leading-relaxed">
			Sei sicuro di voler eliminare definitivamente questo dispositivo dal registro?<br>
			L'operazione non può essere annullata.
		</p>
	</div>

	{#snippet footer()}
		<button onclick={() => (showConfirmModal = false)} class="flex-1 py-3 text-[10px] font-black uppercase text-gray-400 hover:text-gray-600 transition-colors">
			Annulla
		</button>
		<button onclick={confermaEliminazione} class="flex-1 bg-red-500 text-white py-3 rounded-xl text-[10px] font-black uppercase shadow-lg hover:bg-red-600 transition-colors">
			Sì, Elimina
		</button>
	{/snippet}
</ModalCard>

<ModalCard bind:isOpen={showErrorModal} headerClass="bg-red-500" maxWidth="max-w-sm">
	{#snippet title()}
		<AlertCircle size={20} /> <span class="font-black uppercase tracking-tighter">Errore</span>
	{/snippet}

	<div class="py-2 text-center">
		<p class="text-sm font-bold text-gray-600">{errorMessage}</p>
	</div>

	{#snippet footer()}
		<button onclick={() => (showErrorModal = false)} class="w-full bg-red-500 text-white py-3 rounded-xl text-[10px] font-black uppercase shadow-lg hover:bg-red-600 transition-colors">
			Chiudi
		</button>
	{/snippet}
</ModalCard>

<style>
	:global(body) { background-color: #F9FAFB; }
</style>