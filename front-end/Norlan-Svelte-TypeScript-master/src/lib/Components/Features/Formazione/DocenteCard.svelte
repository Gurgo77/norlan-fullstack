<script lang="ts">
	/**
	 * Componente Svelte per la visualizzazione compatta di un Docente.
	 * Mostra l'avatar, il nome, la qualifica principale e un elenco puntato delle specializzazioni o titoli posseduti.
	 */
	import { User } from 'lucide-svelte';

	interface DocenteCardData {
		nome: string;
		qualifica: string;
		titoli: string[];
	}
	// Destrutturazione delle proprietà: riceve in ingresso i dati del docente strutturati secondo l'interfaccia DocenteCardData
	let { docente }: { docente: DocenteCardData } = $props();
</script>

<!-- Contenitore principale della card: applica effetti di hover (sollevamento e bordo colorato) e organizza il layout in colonna flessibile -->
<div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden transition-all duration-300 hover:-translate-y-1 hover:shadow-xl hover:border-[#1B4B6B]/30 h-full flex flex-col">
	<div class="p-6 flex-1 flex flex-col">
		<!-- Intestazione della card con l'avatar stilizzato (icona User) affiancato da nome e qualifica del docente -->
		<div class="flex items-center mb-5">
			<div class="w-14 h-14 mr-4 bg-gray-50 rounded-full flex-shrink-0 flex items-center justify-center border border-gray-200">
				<User class="w-6 h-6 text-[#1B4B6B]" />
			</div>
			<div>
				<h3 class="font-extrabold text-[#1B4B6B] text-base leading-tight uppercase">{docente.nome}</h3>
				<p class="text-xs font-bold text-gray-500 uppercase mt-1">{docente.qualifica}</p>
			</div>
		</div>

		<!-- Sezione dedicata ai titoli: viene renderizzata solo se l'array contiene almeno un elemento, mostrandoli come lista puntata -->
		{#if docente.titoli && docente.titoli.length > 0}
			<div class="mt-auto pt-4 border-t border-gray-100">
				<p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-3">Qualifiche e Titoli</p>
				<ul class="space-y-2">
					{#each docente.titoli as titolo}
						<li class="text-[11px] font-medium text-gray-600 flex items-start gap-2 leading-tight">
							<span class="w-1.5 h-1.5 rounded-full bg-[#1B4B6B]/60 mt-1 flex-shrink-0"></span>
							<span>{titolo}</span>
						</li>
					{/each}
				</ul>
			</div>
		{/if}
	</div>
</div>