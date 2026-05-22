<script lang="ts">
    /**
     * Componente Alert/Card flessibile.
     * Supporta 4 varianti cromatiche e può comportarsi come un link cliccabile o come un blocco statico.
     */
    import type { ComponentType } from 'svelte';

    type VarianteAlert = 'danger' | 'warning' | 'info' | 'success';

    // Destrutturazione delle proprietà (Svelte 5): testi, icona dinamica, URL e stile del componente
    let {
        titolo,
        sottotitolo = '',
        stato = '',
        data = '',
        icona: Icon,
        variante = 'warning',
        href = ''
    } = $props<{
        titolo: string;
        sottotitolo?: string;
        stato?: string;
        data?: string;
        icona: ComponentType;
        variante?: VarianteAlert;
        href?: string;
    }>();

    // Raccogliamo qui tutti i colori e gli stili per mantenere l'HTML pulito e facile da leggere.
    const temi: Record<VarianteAlert, {
        wrapper: string;
        iconBox: string;
        iconBase: string;
        title: string;
        sub: string;
        stato: string;
    }> = {
        danger: {
            wrapper: 'bg-red-50 border-red-100 hover:bg-red-100',
            iconBox: 'bg-white text-red-500',
            iconBase: 'text-red-500',
            title: 'text-red-700',
            sub: 'text-red-500',
            stato: 'text-red-600'
        },
        warning: {
            wrapper: 'bg-amber-50 border-amber-100 hover:bg-amber-100',
            iconBox: 'bg-white text-amber-500',
            iconBase: 'text-amber-500',
            title: 'text-[#1B4B6B]',
            sub: 'text-gray-500',
            stato: 'text-amber-600'
        },
        info: {
            wrapper: 'bg-blue-50 border-blue-100 hover:bg-blue-100',
            iconBox: 'bg-white text-blue-500',
            iconBase: 'text-blue-500',
            title: 'text-[#1B4B6B]',
            sub: 'text-gray-500',
            stato: 'text-blue-600'
        },
        success: {
            wrapper: 'bg-emerald-50 border-emerald-100 hover:bg-emerald-100',
            iconBox: 'bg-white text-emerald-500',
            iconBase: 'text-emerald-500',
            title: 'text-emerald-700',
            sub: 'text-emerald-500',
            stato: 'text-emerald-600'
        }
    };

    const t = $derived(temi[variante as VarianteAlert]);
</script>


<!-- Se abbiamo passato un link creiamo un tag <a> per poterci cliccare, altrimenti usiamo un normale <div>. -->
{#if href}
    <a {href} class="block w-full p-4 rounded-2xl border transition-colors group {t.wrapper}">
        <div class="flex items-start gap-3">
            <div class="p-2 rounded-lg shrink-0 shadow-sm {t.iconBox}">
                <Icon size={16} />
            </div>
            <div class="flex-1">
                <p class="text-xs font-black uppercase leading-tight {t.title}">{titolo}</p>
                {#if sottotitolo}
                    <p class="text-[9px] font-bold uppercase mt-1 {t.sub}">{sottotitolo}</p>
                {/if}
            </div>
            {#if stato || data}
                <div class="text-right shrink-0">
                    {#if stato && stato.trim() !== ''}
                        <span class="text-[10px] font-black uppercase block {t.stato}">{stato}</span>
                    {/if}
                    {#if data}
                        <span class="text-[9px] font-bold text-gray-400 uppercase">{data}</span>
                    {/if}
                </div>
            {/if}
        </div>
    </a>
{:else}
    <div class="w-full flex items-center justify-between p-4 rounded-2xl border transition-colors {t.wrapper}">
        <div class="flex items-center gap-4">
            <Icon size={18} class="shrink-0 {t.iconBase}" />
            <div>
                <span class="text-xs font-black uppercase block {t.title}">{titolo}</span>
                {#if sottotitolo}
                    <span class="text-[9px] font-bold uppercase {t.sub}">{sottotitolo}</span>
                {/if}
            </div>
        </div>
        {#if stato || data}
            <div class="text-right shrink-0">
                {#if stato && stato.trim() !== ''}
                    <span class="text-[10px] font-black uppercase block {t.stato}">{stato}</span>
                {/if}
                {#if data}
                    <span class="text-[9px] font-bold text-gray-400 uppercase">{data}</span>
                {/if}
            </div>
        {/if}
    </div>
{/if}
