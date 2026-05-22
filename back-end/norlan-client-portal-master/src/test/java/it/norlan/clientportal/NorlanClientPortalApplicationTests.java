package it.norlan.clientportal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
/**
 * Suite di collaudo (Integration Test) di base per il bootstrap dell'infrastruttura.
 * Funge da Smoke Test architetturale per garantire che il container IoC (Inversion of Control)
 * di Spring riesca a risolvere l'intero grafo delle dipendenze senza conflitti, validando il setup ambientale.
 */
@SpringBootTest
class NorlanClientPortalApplicationTests {

    @Test
    void contextLoads() {
    }

}
