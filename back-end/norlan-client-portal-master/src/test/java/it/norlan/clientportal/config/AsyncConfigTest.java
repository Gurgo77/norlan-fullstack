package it.norlan.clientportal.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
/**
 * Suite di collaudo (Unit Test) per l'infrastruttura di esecuzione asincrona.
 * Verifica rigorosamente il dimensionamento del Thread Pool per prevenire l'esaurimento
 * delle risorse di sistema e testa i meccanismi di Fault Tolerance (Resilienza) dei thread in background.
 */

class AsyncConfigTest {

    private AsyncConfig asyncConfig;

    @BeforeEach
    void setUp() {
        asyncConfig = new AsyncConfig();
    }

    // Verifica il corretto tuning prestazionale del Thread Pool (Core e Max Size) per garantire scalabilità concorrente proteggendo la CPU da sovraccarichi
    @Test
    void notificheExecutor_ConfiguraCorrettamenteIlPoolDiThread() {
        Executor executor = asyncConfig.notificheExecutor();

        assertNotNull(executor, "Il bean Executor non deve essere nullo");
        assertTrue(executor instanceof ThreadPoolTaskExecutor, "L'executor deve essere un'istanza di ThreadPoolTaskExecutor");

        ThreadPoolTaskExecutor taskExecutor = (ThreadPoolTaskExecutor) executor;

        assertEquals(5, taskExecutor.getCorePoolSize(), "La Core Pool Size deve essere configurata a 5");
        assertEquals(10, taskExecutor.getMaxPoolSize(), "La Max Pool Size deve essere configurata a 10");
        assertEquals("NotificaThread-", taskExecutor.getThreadNamePrefix(), "Il prefisso del thread deve corrispondere allo standard definito");
    }

    // Sfrutta la Java Reflection per simulare un crash di sistema asincrono, verificando che l'handler intercetti l'anomalia in modalità "Fail-Safe"
    @Test
    void getAsyncUncaughtExceptionHandler_GestisceEccezioniSenzaInterruzioni() throws NoSuchMethodException {
        AsyncUncaughtExceptionHandler handler = asyncConfig.getAsyncUncaughtExceptionHandler();
        assertNotNull(handler, "L'handler delle eccezioni asincrone non deve essere nullo");

        Throwable eccezioneSimulata = new RuntimeException("Simulazione errore thread asincrono");
        Method metodoSimulato = AsyncConfigTest.class.getDeclaredMethod("metodoDummyPerTest");
        Object[] parametriSimulati = new Object[]{"Parametro di Test", 42};

        assertDoesNotThrow(() ->
                        handler.handleUncaughtException(eccezioneSimulata, metodoSimulato, parametriSimulati),
                "L'handler deve processare e loggare l'errore senza sollevare nuove eccezioni"
        );
    }

    private void metodoDummyPerTest() {
    }
}
