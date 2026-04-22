<script lang="ts">
	interface Course {
		title: string;
		reference: string;
		content: { level: string; description: string; risk: string; }[];
	}

	let { corso }: { corso: Course } = $props();
</script>

<div class="bg-white rounded-lg shadow-md mb-6 overflow-hidden border border-gray-100 transition-all hover:shadow-lg">
	<div class="bg-[#1B4B6B] text-white p-4">
		<h3 class="text-xl font-bold">{corso.title}</h3>
		<p class="text-sm opacity-80">{corso.reference}</p>
	</div>
	<div class="p-6">
		<div class="grid grid-cols-1 md:grid-cols-3 gap-6">
			{#each corso.content as item (item.level)}
				<div class="border-l-4 pl-4 {item.risk === 'low' ? 'border-blue-200' : item.risk === 'medium' ? 'border-yellow-400' : 'border-red-500'}">
					<h4 class="font-bold text-[#1B4B6B]">{item.level}</h4>
					<p class="text-sm text-gray-600 mt-2">{item.description}</p>
				</div>
			{/each}
		</div>
	</div>
</div>