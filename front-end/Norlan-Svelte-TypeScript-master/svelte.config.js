import adapter from '@sveltejs/adapter-node';
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';
/**
 * Configurazione base di SvelteKit con preprocessore Vite per supportare TypeScript.
 * Utilizza l'adapter-node per generare un server Node.js standalone pronto per la produzione.
 */
const config = {
	preprocess: vitePreprocess(),

	kit: {
		adapter: adapter()
	}
};

export default config;
