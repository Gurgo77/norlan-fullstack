<script lang="ts">
    import type { Notifica } from '$lib/models/Notifica';

    interface Props {
        notifica: Notifica;
        onLeggi: (id: number) => void;
    }

    let { notifica, onLeggi }: Props = $props();
</script>

<button
        class="w-full text-left p-4 border-b border-gray-50 hover:bg-blue-50/50 transition-colors cursor-pointer group block focus:outline-none"
        onclick={() => onLeggi(notifica.idNotifica)}
>
    <div class="flex items-start gap-3">
        <div class="w-2 h-2 rounded-full bg-[#1B4B6B] mt-1.5 shrink-0 transition-colors group-hover:bg-blue-600"></div>

        <div class="flex-1">
            <p class="text-xs font-bold text-gray-700 leading-tight mb-1 group-hover:text-[#1B4B6B] transition-colors">
                {notifica.messaggio}
            </p>
            <p class="text-[9px] font-black text-gray-400 uppercase tracking-wide">
                {new Date(notifica.dataInvio).toLocaleString('it-IT')}
            </p>
        </div>
    </div>
</button>