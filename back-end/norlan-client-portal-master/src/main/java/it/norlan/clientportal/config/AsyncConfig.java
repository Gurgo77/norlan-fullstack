package it.norlan.clientportal.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

/**
 * Configurazione per la gestione dell'esecuzione asincrona (Multithreading).
 * Abilita l'uso dell'annotazione @Async in Spring Boot per svincolare
 * task lenti (es. invio email o push notification) dal thread principale.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Bean(name = "notificheExecutor")
    public Executor notificheExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // Pool dedicato alle notifiche: gestisce max 10 thread e accoda fino a 100 richieste
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("NotificaThread-");

        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {

            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                // Intercetta e stampa nei log eventuali errori sfuggiti nei thread in background
                System.err.println("Errore nel thread asincrono;");
                System.err.println("Metodo fallito: " + method.getName());
                System.err.println("Causa dell'errore: " + ex.getMessage());

                for (Object param : params) {
                    System.err.println("Parametro passato: " + param);
                }
            }
        };
    }
}