package it.norlan.clientportal.config;

import it.norlan.clientportal.security.CustomUserDetailsService;
import it.norlan.clientportal.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
/**
 * Suite di collaudo (Unit Test) per l'infrastruttura di comunicazione Real-Time (WebSocket/STOMP).
 * Valida il routing del Message Broker e applica rigorose tecniche di Negative Testing per verificare
 * che l'intercettore di sicurezza respinga le connessioni Socket sprovviste di Token JWT valido.
 */
@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private WebSocketConfig webSocketConfig;

    @BeforeEach
    void setUp() {
    }

    // Verifica la corretta registrazione del tunnel bidirezionale e l'attivazione del meccanismo di fallback (SockJS) per i client non compatibili
    @Test
    void registerStompEndpoints_ConfiguraCorrettamenteEndpointESockJS() {
        StompEndpointRegistry registryMock = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration registrationMock = mock(StompWebSocketEndpointRegistration.class);

        when(registryMock.addEndpoint("/ws")).thenReturn(registrationMock);
        when(registrationMock.setAllowedOriginPatterns("*")).thenReturn(registrationMock);

        webSocketConfig.registerStompEndpoints(registryMock);

        verify(registryMock).addEndpoint("/ws");
        verify(registrationMock).setAllowedOriginPatterns("*");
        verify(registrationMock).withSockJS();
    }

    @Test
    void configureMessageBroker_ImpostaPrefissiEDestinazioni() {
        MessageBrokerRegistry registryMock = mock(MessageBrokerRegistry.class);

        webSocketConfig.configureMessageBroker(registryMock);

        verify(registryMock).enableSimpleBroker("/user", "/topic");
        verify(registryMock).setApplicationDestinationPrefixes("/app");
        verify(registryMock).setUserDestinationPrefix("/user");
    }

    // Negative Testing: simula un tentativo di accesso non autorizzato al protocollo STOMP, garantendo che l'intercettore blocchi l'handshake (Fail-Fast)
    @Test
    void channelInterceptor_ConnessioneSenzaToken_LanciaEccezione() {
        ChannelInterceptor interceptor = estraiIntercettoreDiSicurezza();
        Message<?> message = creaMessaggioStomp(StompCommand.CONNECT, null);
        MessageChannel channel = mock(MessageChannel.class);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> interceptor.preSend(message, channel)
        );
        assertEquals("Token JWT Mancante nella connessione STOMP", exception.getMessage());
    }

    @Test
    void channelInterceptor_ConnessioneConTokenScaduto_LanciaEccezione() {
        ChannelInterceptor interceptor = estraiIntercettoreDiSicurezza();
        Message<?> message = creaMessaggioStomp(StompCommand.CONNECT, "Bearer tokenScaduto");
        MessageChannel channel = mock(MessageChannel.class);

        when(jwtUtil.extractUsername("tokenScaduto")).thenReturn("test@norlan.it");
        UserDetails mockUser = new User("test@norlan.it", "pwd", Collections.emptyList());
        when(userDetailsService.loadUserByUsername("test@norlan.it")).thenReturn(mockUser);
        when(jwtUtil.validateToken("tokenScaduto", mockUser)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> interceptor.preSend(message, channel)
        );
        assertEquals("Token JWT Scaduto o Non Valido", exception.getMessage());
    }

    @Test
    void channelInterceptor_ConnessioneConTokenValido_IniettaUtente() {
        ChannelInterceptor interceptor = estraiIntercettoreDiSicurezza();
        Message<?> message = creaMessaggioStomp(StompCommand.CONNECT, "Bearer tokenValido");
        MessageChannel channel = mock(MessageChannel.class);

        UserDetails mockUser = new User("admin@norlan.it", "pwd", Collections.emptyList());
        when(jwtUtil.extractUsername("tokenValido")).thenReturn("admin@norlan.it");
        when(userDetailsService.loadUserByUsername("admin@norlan.it")).thenReturn(mockUser);
        when(jwtUtil.validateToken("tokenValido", mockUser)).thenReturn(true);

        Message<?> elaborato = interceptor.preSend(message, channel);

        assertNotNull(elaborato);
        StompHeaderAccessor elaboratoAccessor = StompHeaderAccessor.getAccessor(elaborato, StompHeaderAccessor.class);
        assertNotNull(elaboratoAccessor.getUser(), "L'utente autenticato deve essere iniettato nell'accessor");
        assertEquals("admin@norlan.it", elaboratoAccessor.getUser().getName());
    }

    @Test
    void channelInterceptor_ComandoDiversoDaConnect_LasciaPassare() {
        ChannelInterceptor interceptor = estraiIntercettoreDiSicurezza();
        Message<?> message = creaMessaggioStomp(StompCommand.SEND, null);
        MessageChannel channel = mock(MessageChannel.class);

        Message<?> elaborato = interceptor.preSend(message, channel);

        assertEquals(message, elaborato, "Un comando diverso da CONNECT non deve essere intercettato dalla logica JWT");
    }

    private ChannelInterceptor estraiIntercettoreDiSicurezza() {
        ChannelRegistration registrationMock = mock(ChannelRegistration.class);
        ArgumentCaptor<ChannelInterceptor> captor = ArgumentCaptor.forClass(ChannelInterceptor.class);

        webSocketConfig.configureClientInboundChannel(registrationMock);
        verify(registrationMock).interceptors(captor.capture());

        return captor.getValue();
    }

    private Message<?> creaMessaggioStomp(StompCommand command, String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (token != null) {
            accessor.setNativeHeader("Authorization", token);
        }

        accessor.setLeaveMutable(true);

        return new GenericMessage<>(new byte[0], accessor.getMessageHeaders());
    }
}
