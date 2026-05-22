<script lang="ts">
    /**
     * Componente Svelte che rappresenta una singola "bolla" di messaggio all'interno di una chat.
     * Gestisce dinamicamente l'allineamento, lo stile visivo (mittente vs destinatario) e visualizza
     * i metadati come l'orario di invio.
     */
    import { Check, CheckCheck } from 'lucide-svelte';

    interface Props {
        testo: string;
        data: string;
        inviatoDaMe: boolean;
        letto?: boolean;
        nomeMittente?: string;
    }

    let {
        testo,
        data,
        inviatoDaMe,
        letto = false,
        nomeMittente = ''
    }: Props = $props();
</script>

<!-- Contenitore flessibile principale: allinea la bolla a destra se il messaggio è nostro, a sinistra altrimenti -->
<div class="flex w-full {inviatoDaMe ? 'justify-end' : 'justify-start'} mb-4">
    <div class="flex flex-col {inviatoDaMe ? 'items-end' : 'items-start'} max-w-[85%] md:max-w-[65%]">

        <!-- Mostra il nome del mittente (in piccolo sopra la bolla) solo se il messaggio è stato ricevuto da un'altra persona -->
        {#if !inviatoDaMe && nomeMittente}
            <span class="text-[9px] font-bold text-gray-400 uppercase ml-2 mb-1 tracking-widest">{nomeMittente}</span>
        {/if}

        <!-- Corpo visivo del messaggio con stili arrotondati condizionali (sfondo blu se inviato, bianco se ricevuto) -->
        <div class="relative px-5 py-3 shadow-sm {inviatoDaMe ?
            'bg-[#1B4B6B] text-white rounded-3xl rounded-tr-sm' :
            'bg-white border border-gray-100 text-[#1B4B6B] rounded-3xl rounded-tl-sm'}">

            <p class="text-[13px] font-medium leading-relaxed whitespace-pre-wrap break-words">{testo}</p>

            <!-- Area inferiore della bolla dedicata ai metadati: mostra l'orario e le spunte di recapito/lettura per i propri messaggi -->
            <div class="flex items-center gap-1.5 mt-2 justify-end {inviatoDaMe ? 'text-white/60' : 'text-gray-400'}">
                <span class="text-[9px] font-bold uppercase tracking-widest">{data}</span>

                {#if inviatoDaMe}
                    {#if letto}
                        <CheckCheck size={14} class="text-blue-300" />
                    {:else}
                        <Check size={14} />
                    {/if}
                {/if}
            </div>
        </div>
    </div>
</div>