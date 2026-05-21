package it.norlan.clientportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Classe di Bootstrap (Entry Point) dell'applicazione Spring Boot.
 * Inizializza il contesto di Spring, avvia il server embedded (Tomcat) e abilita globalmente
 * l'esecuzione dei task asincroni programmati (Cron Jobs) tramite l'annotazione @EnableScheduling.
 */

@SpringBootApplication
@EnableScheduling
public class NorlanClientPortalApplication {

    // Metodo di avvio standard Java: delega a SpringApplication la costruzione del ciclo di vita dell'intero framework
    public static void main(String[] args) {
        SpringApplication.run(NorlanClientPortalApplication.class, args);
    }

}
