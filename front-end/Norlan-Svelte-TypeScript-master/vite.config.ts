import tailwindcss from '@tailwindcss/vite';
import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vite';
/**
 * Configurazione principale di Vite per il progetto SvelteKit.
 * Integra i plugin necessari per la compilazione del framework (SvelteKit)
 * e per l'elaborazione degli stili utility-first (Tailwind CSS).
 */
export default defineConfig({
	plugins: [tailwindcss(), sveltekit()],
	define: {
		global: 'window'
	}
});