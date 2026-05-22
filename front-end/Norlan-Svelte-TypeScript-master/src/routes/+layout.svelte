<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import Navbar from "$lib/Components/Layout/Navbar.svelte";
	import Footer from "$lib/Components/Layout/Footer.svelte";
	import ProgressBar from "$lib/Components/Layout/ProgressBar.svelte";
	import "../app.css";

	/*
Layout principale dell'applicazione (Root Layout).
Coordina la struttura globale (Navbar, Footer, ProgressBar) e implementa la logica
di rendering condizionale per isolare le viste del dashboard dal layout pubblico.
Gestisce inoltre il calcolo reattivo della barra di progresso basato sullo scroll.
*/

	// Riceve il contenuto delle pagine figlie da renderizzare all'interno del main layout
	let { children } = $props();
	let progress = $state(0);

	// Deriva lo stato di visualizzazione per nascondere componenti di layout standard nelle viste protette (Dashboard)
	let isDashboard = $derived($page.url.pathname.startsWith('/dashboard'));

	// Inizializza il listener per il monitoraggio dello scroll verticale e aggiorna la barra di avanzamento
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
	<!-- Navbar e Footer vengono visualizzati selettivamente in base al percorso corrente (esclusi per la Dashboard) -->
	{#if !isDashboard}
		<Navbar />
	{/if}

	<!-- Barra di avanzamento globale posizionata sopra il contenuto principale -->
	<ProgressBar {progress} />

	<!-- Area dinamica per il rendering delle pagine -->
	<main class="flex-grow">
		{@render children()}
	</main>

	{#if !isDashboard}
		<Footer />
	{/if}
</div>