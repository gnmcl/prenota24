package com.prenota24.backend.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Impedisce l'avvio se il JWT secret di sviluppo (pubblico, nel repository) viene usato
 * fuori dai profili di sviluppo: con quel secret chiunque potrebbe firmare token validi.
 */
@Component
@RequiredArgsConstructor
public class JwtSecretGuard implements InitializingBean {

    static final String DEV_SECRET_PREFIX = "dev-only-";
    private static final Profiles DEV_PROFILES = Profiles.of("dev", "devmessaging");

    private final JwtProperties jwtProperties;
    private final Environment environment;

    @Override
    public void afterPropertiesSet() {
        if (jwtProperties.secret().startsWith(DEV_SECRET_PREFIX) && !environment.acceptsProfiles(DEV_PROFILES)) {
            throw new IllegalStateException(
                    "JWT secret di sviluppo usato fuori dai profili dev: imposta JWT_SECRET con un valore segreto.");
        }
    }
}
