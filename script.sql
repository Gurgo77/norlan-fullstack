create table log_sincronizzazione
(
    id_log             int auto_increment
        primary key,
    descrizione_evento varchar(255)                         not null,
    data_evento        timestamp  default CURRENT_TIMESTAMP null,
    esito_positivo     tinyint(1) default 1                 null,
    note_tecniche      text                                 null
);

create table utente
(
    id_utente                int auto_increment
        primary key,
    email                    varchar(255)                                       not null,
    password_hash            varchar(255)                                       not null,
    ruolo                    enum ('ADMIN', 'DOCENTE', 'AZIENDA', 'DIPENDENTE') not null,
    richiede_cambio_password tinyint(1) default 1                               not null,
    constraint email
        unique (email)
);

create table admin
(
    id_utente int not null
        primary key,
    constraint fk_admin_utente
        foreign key (id_utente) references utente (id_utente)
            on delete cascade
);

create table azienda
(
    id_utente           int          not null
        primary key,
    ragione_sociale     varchar(255) not null,
    partita_iva         varchar(11)  not null,
    sede_legale         varchar(255) null,
    pec                 varchar(255) null,
    telefono            varchar(20)  null,
    cellulare           varchar(20)  null,
    referente_aziendale varchar(255) null,
    constraint partita_iva
        unique (partita_iva),
    constraint fk_azienda_utente
        foreign key (id_utente) references utente (id_utente)
            on delete cascade
);

create table dipendente
(
    id_utente      int          not null
        primary key,
    codice_fiscale varchar(16)  not null,
    id_azienda     int          not null,
    nome           varchar(255) not null,
    cognome        varchar(255) not null,
    constraint codice_fiscale
        unique (codice_fiscale),
    constraint fk_dipendente_azienda
        foreign key (id_azienda) references azienda (id_utente),
    constraint fk_dipendente_utente
        foreign key (id_utente) references utente (id_utente)
            on delete cascade
);

create table assegnazione_dpi
(
    id_assegnazione         int auto_increment
        primary key,
    id_dipendente           int                                                                          not null,
    tipo                    enum ('ELMETTO', 'GUANTI', 'SCARPE_ANTINFORTUNISTICHE', 'OCCHIALI', 'ALTRO') not null,
    data_consegna           date default (curdate())                                                     null,
    data_scadenza_revisione date                                                                         not null,
    nome_dpi_personalizzato varchar(255)                                                                 null,
    constraint fk_dpi_dipendente
        foreign key (id_dipendente) references dipendente (id_utente)
            on delete cascade
);

create table docente
(
    id_utente                int          not null
        primary key,
    specializzazione_tecnica varchar(255) null,
    nome                     varchar(255) not null,
    cognome                  varchar(255) not null,
    constraint fk_docente_utente
        foreign key (id_utente) references utente (id_utente)
            on delete cascade
);

create table corso_formazione
(
    id_corso     int auto_increment
        primary key,
    titolo       varchar(255) not null,
    data_orario  timestamp    not null,
    luogo_fisico varchar(255) not null,
    id_docente   int          not null,
    stato        varchar(255) null,
    constraint fk_corso_docente
        foreign key (id_docente) references docente (id_utente)
);

create table documento
(
    id_documento     int auto_increment
        primary key,
    id_azienda       int                                                                                              not null,
    modulo           enum ('PRIVACY', 'SICUREZZA', 'HACCP')                                                           not null,
    tipologia        enum ('DVR', 'HACCP', 'NOMINA_RESPONSABILE', 'REGISTRO_TRATTAMENTI', 'ALTRO', 'ATTESTATO_CORSO') not null,
    stato            enum ('CARICATO', 'IN_ATTESA_FIRMA', 'APPROVATO', 'ARCHIVIATO') default 'CARICATO'               null,
    file_path        varchar(255)                                                                                     not null,
    data_caricamento date                                                            default (curdate())              null,
    data_scadenza    date                                                                                             not null,
    constraint fk_documento_azienda
        foreign key (id_azienda) references azienda (id_utente)
);

create table iscrizione_corso
(
    id_utente           int                  not null,
    id_corso            int                  not null,
    presenza_confermata tinyint(1) default 0 null,
    id_documento        int                  null,
    primary key (id_utente, id_corso),
    constraint fk_iscrizione_corso
        foreign key (id_corso) references corso_formazione (id_corso),
    constraint fk_iscrizione_documento
        foreign key (id_documento) references documento (id_documento),
    constraint fk_iscrizione_utente
        foreign key (id_utente) references utente (id_utente)
);

create table feedback_corso
(
    id_feedback      int auto_increment
        primary key,
    id_utente        int                                 not null,
    id_corso         int                                 not null,
    rating_docenza   int                                 not null,
    rating_contenuti int                                 not null,
    commento         text                                null,
    data_creazione   timestamp default CURRENT_TIMESTAMP null,
    constraint uq_feedback_iscrizione
        unique (id_utente, id_corso),
    constraint fk_feedback_iscrizione
        foreign key (id_utente, id_corso) references iscrizione_corso (id_utente, id_corso)
            on delete cascade,
    constraint chk_rating_contenuti
        check (`rating_contenuti` between 1 and 5),
    constraint chk_rating_docenza
        check (`rating_docenza` between 1 and 5)
);

create table materiale_didattico
(
    id_materiale     int auto_increment
        primary key,
    id_corso         int                                 not null,
    titolo_documento varchar(255)                        not null,
    percorso_file    varchar(255)                        not null,
    data_caricamento timestamp default CURRENT_TIMESTAMP null,
    constraint fk_materiale_corso
        foreign key (id_corso) references corso_formazione (id_corso)
            on delete cascade
);

create table messaggio
(
    id_messaggio    int auto_increment
        primary key,
    id_mittente     int                                 not null,
    id_destinatario int                                 not null,
    testo           text                                not null,
    timestamp_invio timestamp default CURRENT_TIMESTAMP not null,
    letto           tinyint(1)                          null,
    constraint fk_messaggio_destinatario
        foreign key (id_destinatario) references utente (id_utente),
    constraint fk_messaggio_mittente
        foreign key (id_mittente) references utente (id_utente)
);

create table notifica
(
    id_notifica            int auto_increment
        primary key,
    id_utente_destinatario int                                                       not null,
    messaggio              text                                                      not null,
    letta                  tinyint(1)                      default 0                 null,
    priorita               enum ('BASSA', 'MEDIA', 'ALTA') default 'MEDIA'           null,
    data_invio             timestamp                       default CURRENT_TIMESTAMP null,
    version                bigint                          default 0                 not null,
    canale                 varchar(255)                    default 'IN_APP'          not null,
    constraint fk_notifica_utente
        foreign key (id_utente_destinatario) references utente (id_utente)
            on delete cascade
);

create table richiesta_rinnovo_documento
(
    id_richiesta   int auto_increment
        primary key,
    id_documento   int                                                                          not null,
    data_richiesta timestamp                                          default CURRENT_TIMESTAMP null,
    stato          enum ('IN_ATTESA', 'IN_LAVORAZIONE', 'COMPLETATO') default 'IN_ATTESA'       null,
    constraint fk_rinnovo_documento
        foreign key (id_documento) references documento (id_documento)
            on delete cascade
);

INSERT INTO utente (email, password_hash, ruolo, richiede_cambio_password) 
VALUES (
    'admin@norlan.it', 
    '$2a$10$r3WTA1rIZYDvxTxCES25R.vzxl..U02e/onPT2TXI9l0hTw8vmU/q', 
    'ADMIN', 
    0
);

INSERT INTO admin (id_utente) 
VALUES (LAST_INSERT_ID());


