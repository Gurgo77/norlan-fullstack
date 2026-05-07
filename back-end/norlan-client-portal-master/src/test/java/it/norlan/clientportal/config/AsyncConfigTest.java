package it.norlan.clientportal.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class AsyncConfigTest {

    private AsyncConfig asyncConfig;

    @BeforeEach
    void setUp() {
        asyncConfig = new AsyncConfig();
    }

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
