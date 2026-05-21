<script lang="ts">
    /**
     * Componente UI per la visualizzazione di dettaglio di un'anagrafica (es. Dipendente o Docente).
     * Genera dinamicamente un'intestazione con pulsanti di azione e una griglia di attributi
     * passati tramite la proprietà 'items'.
     */
    import { fade } from 'svelte/transition';
    import {
        Building2, Edit3, Mail, MessageSquare,
        Trash2, RefreshCw
    } from 'lucide-svelte';
    import type { Component } from 'svelte';

    interface DetailItem {
        label: string;
        value: string | number | undefined;
        icon: Component;
        isMono?: boolean;
    }

    let {
        nome,
        cognome,
        sottotitolo,
        items = [],
        onEdit,
        onUpdate,
        onMail,
        onContact,
        onDelete
    } = $props<{
        nome: string;
        cognome: string;
        sottotitolo?: string;
        items: DetailItem[];
        onEdit?: () => void;
        onUpdate?: () => void;
        onMail?: () => void;
        onContact?: () => void;
        onDelete?: () => void;
    }>();
</script>

<div class="bg-white rounded-3xl shadow-xl border border-gray-100 overflow-hidden mb-12" in:fade>
    <div class="bg-[#1B4B6B] p-6 md:p-10 text-white flex flex-col md:flex-row justify-between items-start md:items-end gap-6 relative">
        <div class="flex items-center gap-4 md:gap-6 w-full md:w-auto">
            <div class="w-16 h-16 md:w-24 md:h-24 bg-white text-[#1B4B6B] rounded-2xl md:rounded-3xl flex items-center justify-center font-black text-3xl md:text-4xl shadow-lg shrink-0">
                {nome?.[0] || ''}{cognome?.[0] || ''}
            </div>
            <div class="min-w-0 flex-1">
                {#if sottotitolo}
                    <div class="flex items-center gap-3 mb-2 md:mb-3">
                        <span class="bg-white/20 border border-white/20 text-white text-[9px] md:text-[10px] font-black px-3 md:px-4 py-1 md:py-1.5 rounded-full uppercase flex items-center gap-2 truncate">
                            <Building2 size={12}/> {sottotitolo}
                        </span>
                    </div>
                {/if}
                <h1 class="text-2xl md:text-5xl font-extrabold uppercase tracking-tighter truncate">{nome} {cognome}</h1>
            </div>
        </div>

        <div class="flex flex-wrap items-center gap-2 w-full md:w-auto mt-2 md:mt-0">
            {#if onUpdate}
                <button onclick={onUpdate} class="flex-1 md:flex-none justify-center flex items-center gap-2 bg-white/20 text-white border border-white/20 px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-white/30 hover:scale-105 whitespace-nowrap">
                    <RefreshCw size={16} /> Aggiorna
                </button>
            {/if}

            {#if onEdit}
                <button onclick={onEdit} class="flex-1 md:flex-none justify-center flex items-center gap-2 bg-white text-[#1B4B6B] px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-gray-100 hover:scale-105 whitespace-nowrap">
                    <Edit3 size={16} /> Modifica
                </button>
            {/if}

            {#if onMail}
                <button onclick={onMail} class="flex-1 md:flex-none justify-center flex items-center gap-2 bg-white/20 text-white border border-white/20 px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-white/30 hover:scale-105 whitespace-nowrap">
                    <Mail size={16} /> Mail
                </button>
            {/if}

            {#if onContact}
                <button onclick={onContact} class="flex-1 md:flex-none justify-center flex items-center gap-2 bg-white/20 text-white border border-white/20 px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] shadow-xl hover:bg-white/30 hover:scale-105 whitespace-nowrap">
                    <MessageSquare size={16} /> Contatta
                </button>
            {/if}

            {#if onDelete}
                <button onclick={onDelete} class="flex-1 md:flex-none justify-center flex items-center gap-2 bg-red-600 text-white px-6 py-3.5 rounded-2xl transition-all font-extrabold uppercase text-[10px] border border-white/10 shadow-xl hover:bg-red-700 hover:scale-105 whitespace-nowrap">
                    <Trash2 size={16} /> Rimuovi
                </button>
            {/if}
        </div>
    </div>

    <div class="p-6 md:p-8 bg-gray-50/30">
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-6">
            {#each items as item}
                <div class="bg-white p-4 md:p-5 rounded-2xl border border-gray-100 shadow-sm transition-all hover:shadow-md">
                    <p class="text-[10px] font-bold text-gray-400 uppercase mb-2 flex items-center gap-2">
                        <item.icon size={12} class="text-[#1B4B6B]"/> {item.label}
                    </p>
                    <p class="text-xs md:text-sm font-bold text-[#1B4B6B] {item.isMono ? 'font-mono tracking-widest uppercase' : 'lowercase truncate'}">
                        {item.value || 'N/D'}
                    </p>
                </div>
            {/each}
        </div>
    </div>
</div>