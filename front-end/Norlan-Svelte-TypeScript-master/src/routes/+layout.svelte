<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import Navbar from "$lib/Components/Layout/Navbar.svelte";
	import Footer from "$lib/Components/Layout/Footer.svelte";
	import ProgressBar from "$lib/Components/Layout/ProgressBar.svelte";
	import "../app.css";

	let { children } = $props();
	let progress = $state(0);

	let isDashboard = $derived($page.url.pathname.startsWith('/dashboard'));

	onMount(() => {
		const updateProgress = () => {
			const scrollTop = window.scrollY || document.documentElement.scrollTop;
			const scrollHeight = document.documentElement.scrollHeight - window.innerHeight;
			progress = scrollHeight > 0 ? (scrollTop / scrollHeight) * 100 : 0;
		};

		window.addEventListener('scroll', updateProgress, { passive: true });
		return () => window.removeEventListener('scroll', updateProgress);
	});
</script>

<div class="flex flex-col min-h-screen">
	{#if !isDashboard}
		<Navbar />
	{/if}

	<ProgressBar {progress} />

	<main class="flex-grow">
		{@render children()}
	</main>

	<Footer />
</div>