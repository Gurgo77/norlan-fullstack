<script lang="ts">
    /*
Componente modale riutilizzabile e personalizzabile.
Gestisce le transizioni visive e permette di bloccare
opzionalmente la chiusura al clic sullo sfondo.
*/
    import { fade, scale } from 'svelte/transition';
    import { X } from 'lucide-svelte';

    let {
        isOpen = $bindable(false),
        title,
        children,
        footer,
        maxWidth = 'max-w-lg',
        headerClass = 'bg-[#1B4B6B]',
        showCloseButton = true,
        disableClickOutside = false
    } = $props();
    // Forza la chiusura della modale aggiornando lo stato bindato
    function close() {
        isOpen = false;
    }
</script>

{#if isOpen}
    <!-- Overlay scuro che intercetta il clic esterno per la chiusura -->
    <div
            class="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm"
            transition:fade
            onclick={(e) => !disableClickOutside && e.target === e.currentTarget && close()}
    >
        <!-- Contenitore centrale con gestione automatica dello scroll interno -->
        <div
                class="bg-white w-full {maxWidth} rounded-[2.5rem] shadow-2xl flex flex-col max-h-[90vh] overflow-hidden"
                in:scale
        >
            <div class="{headerClass} p-6 text-white flex justify-between items-center shrink-0">
                <div class="flex items-center gap-3">
                    {@render title?.()}
                </div>
                {#if showCloseButton}
                    <button onclick={close} class="text-white hover:rotate-90 transition-all duration-300">
                        <X size={24} />
                    </button>
                {/if}
            </div>
            <!-- Area dedicata al contenuto dinamico passato al componente -->
            <div class="p-8 overflow-y-auto custom-scrollbar flex-1 bg-gray-50/30">
                {@render children?.()}
            </div>

            {#if footer}
                <div class="p-6 bg-white border-t border-gray-100 flex gap-4 shrink-0">
                    {@render footer()}
                </div>
            {/if}
        </div>
    </div>
{/if}

<style>
    .custom-scrollbar::-webkit-scrollbar { width: 5px; }
    .custom-scrollbar::-webkit-scrollbar-thumb { background: #E2E8F0; border-radius: 10px; }
</style>