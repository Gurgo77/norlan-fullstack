package it.norlan.clientportal.model;

/**
 * Enumerazione che definisce gli stati del semaforo di Compliance aziendale.
 * Utilizzato dai servizi di calcolo per fornire un indicatore visivo immediato (Verde, Giallo, Rosso)
 * sullo stato di regolarità di documenti e DPI in base alle imminenti scadenze.
 */

public enum LivelloRischio {
    VERDE,
    GIALLO,
    ROSSO
}
