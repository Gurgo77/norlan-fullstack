package it.norlan.clientportal.security;

import it.norlan.clientportal.model.Utente;
import it.norlan.clientportal.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UtenteRepository utenteRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utente utente = utenteRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Utente non trovato con email: " + email));

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + utente.getRuolo().name());

        return new org.springframework.security.core.userdetails.User(
                utente.getEmail(),
                utente.getPasswordHash(),
                Collections.singletonList(authority)
        );
    }
}
