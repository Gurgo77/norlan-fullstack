<script lang="ts">
    /**
     * Componente Svelte che rappresenta una singola riga cliccabile nella lista delle conversazioni.
     * Mostra l'icona, il nome, un'anteprima dell'ultimo messaggio e l'orario, includendo
     * un badge per i messaggi non letti e stili condizionali se la chat è attualmente selezionata.
     */
    import type { ComponentType } from 'svelte';

    interface Props {
        nomeCompleto: string;
        sottotitolo?: string;
        ultimoMessaggio?: string;
        dataUltimoMessaggio?: string;
        nonLetti?: number;
        selezionato?: boolean;
        icona?: ComponentType | any;
        onClick: () => void;
    }

    let {
        nomeCompleto,
        sottotitolo = '',
        ultimoMessaggio = '',
        dataUltimoMessaggio = '',
        nonLetti = 0,
        selezionato = false,
        icona: Icon,
        onClick
    }: Props = $props();
</script>
<!-- Pulsante intero che racchiude la riga: applica un bordo laterale e uno sfondo dedicato se la chat è selezionata -->
<button
        class="w-full flex items-center gap-4 p-4 border-b border-gray-50 transition-all text-left group focus:outline-none
    {selezionato ? 'bg-[#1B4B6B]/5 border-l-4 border-l-[#1B4B6B]' : 'hover:bg-gray-50 border-l-4 border-l-transparent'}"
        onclick={onClick}
>
    <div class="relative shrink-0">
        <!-- Contenitore dell'icona (avatar) con colori dinamici per l'hover e lo stato di selezione -->
        <div class="w-12 h-12 rounded-2xl flex items-center justify-center transition-colors
            {selezionato ? 'bg-[#1B4B6B] text-white shadow-md' : 'bg-[#1B4B6B]/10 text-[#1B4B6B] group-hover:bg-[#1B4B6B] group-hover:text-white'}">
            {#if Icon}
                <Icon size={20} />
            {/if}
        </div>

        <!-- Badge di notifica rosso per i messaggi non letti, con limite visivo impostato a "99+" se il numero è eccessivo -->
        {#if nonLetti > 0}
            <div class="absolute -top-1.5 -right-1.5 w-5 h-5 bg-red-500 rounded-full flex items-center justify-center text-[9px] font-bold text-white border-2 border-white shadow-sm">
                {nonLetti > 99 ? '99+' : nonLetti}
            </div>
        {/if}
    </div>

    <div class="flex-1 min-w-0">
        <div class="flex justify-between items-baseline mb-0.5">
            <h4 class="text-sm font-extrabold text-[#1B4B6B] truncate pr-2">{nomeCompleto}</h4>
            {#if dataUltimoMessaggio}
                <span class="text-[9px] font-bold text-gray-400 uppercase tracking-widest shrink-0">{dataUltimoMessaggio}</span>
            {/if}
        </div>

        {#if sottotitolo}
            <p class="text-[9px] font-bold text-gray-400 uppercase truncate tracking-wider mb-1">{sottotitolo}</p>
        {/if}

        <!-- Anteprima dell'ultimo messaggio ricevuto: il testo appare in grassetto se la chat contiene messaggi da leggere -->
        {#if ultimoMessaggio}
            <p class="text-xs truncate {nonLetti > 0 ? 'font-bold text-[#1B4B6B]' : 'text-gray-500'}">
                {ultimoMessaggio}
            </p>
        {/if}
    </div>
</button>