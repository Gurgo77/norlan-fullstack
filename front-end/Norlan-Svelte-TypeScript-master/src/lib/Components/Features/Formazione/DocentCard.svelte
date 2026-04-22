<script lang="ts">
	import { User } from 'lucide-svelte';
	import type { Docente } from '$lib/models/Docente';

	let { docente }: { docente: Docente } = $props();
</script>

<div class="bg-white rounded-lg shadow-md overflow-hidden transition-transform duration-300 hover:-translate-y-1 hover:shadow-xl">
	<div class="p-6">
		<div class="flex items-center">
			<div class="w-16 h-16 mr-4 bg-gray-200 rounded-full overflow-hidden flex-shrink-0 flex items-center justify-center">
				<User class="w-8 h-8 text-[#1B4B6B]" />
			</div>
			<div>
				<h3 class="font-bold text-[#1B4B6B] text-lg">{docente.nome} {docente.cognome}</h3>
				<p class="text-sm text-gray-600">{docente.specializzazioneTecnica}</p>
			</div>
		</div>
	</div>
</div>