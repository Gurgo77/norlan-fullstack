<script lang="ts">
    import { Trash2, GraduationCap, BookOpen, ChevronRight } from 'lucide-svelte';
    import { scale } from 'svelte/transition';
    import type { DocenteData } from '$lib/models/Docente';

    let { docente, onclick, onDelete }: {
        docente: DocenteData,
        onclick: () => void,
        onDelete: () => void
    } = $props();
</script>

<div
        in:scale
        class="bg-white rounded-3xl border border-gray-100 shadow-sm hover:shadow-xl hover:-translate-y-1 transition-all group relative flex flex-col h-full overflow-hidden hover:border-[#1B4B6B]/30"
>
    <div
            role="button"
            tabindex="0"
            onclick={onclick}
            onkeydown={(e) => e.key === 'Enter' && onclick()}
            class="p-6 pb-4 cursor-pointer flex-1"
    >
        <button
                onclick={(e) => { e.stopPropagation(); onDelete(); }}
                class="absolute top-4 right-4 p-2 text-gray-300 hover:text-red-600 opacity-0 group-hover:opacity-100 transition-all z-10 hover:bg-red-50 rounded-lg"
                title="Elimina Docente"
        >
            <Trash2 size={18} />
        </button>

        <div class="flex items-center gap-4 mb-5">
            <div class="w-14 h-14 bg-[#1B4B6B]/10 text-[#1B4B6B] rounded-2xl flex items-center justify-center font-black text-lg group-hover:bg-[#1B4B6B] group-hover:text-white transition-all">
                {docente.nome[0]}{docente.cognome[0]}
            </div>
            <div>
                <h3 class="font-extrabold text-[#1B4B6B] text-lg uppercase leading-tight">
                    {docente.nome} {docente.cognome}
                </h3>
                <p class="text-[10px] text-gray-400 font-bold uppercase mt-1 flex items-center gap-1">
                    <GraduationCap size={12}/> Formatore Specialist
                </p>
            </div>
        </div>

        <div class="space-y-3 pt-4 border-t border-gray-50">
            <div>
                <p class="text-[9px] text-gray-400 font-bold uppercase mb-0.5">Specializzazione Tecnica</p>
                <p class="text-[11px] font-black text-[#1B4B6B] uppercase leading-tight truncate">
                    {docente.specializzazioneTecnica || 'Non specificata'}
                </p>
            </div>
        </div>
    </div>

    <button
            onclick={onclick}
            class="mt-auto w-full p-6 pt-4 border-t border-gray-50 flex justify-between items-center hover:bg-gray-50/50 transition-colors"
    >
        <div class="flex items-center gap-2">
            <BookOpen size={16} class="text-[#1B4B6B]"/>
            <span class="text-[10px] font-bold text-gray-400 uppercase italic">Vedi Scheda Docente</span>
        </div>
        <ChevronRight size={20} class="text-[#1B4B6B]" />
    </button>
</div>