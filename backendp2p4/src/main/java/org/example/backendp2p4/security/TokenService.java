package org.example.backendp2p4.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;

@Service
@AllArgsConstructor
public class TokenService {

    private final JwtConfig jwtConfig;

    public String generateToken(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Header
        var header = new JWSHeader.Builder(jwtConfig.getAlgorithm())
                .type(JOSEObjectType.JWT)
                .build();

        // Fechas
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtConfig.getJwtExpiration());

        // Claims
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("TotalSoft")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiry))
                .claim("name", userDetails.getUsuario().getNombre())
                .claim("rol", userDetails.getUsuario().getRol())
                .build();

        // Crear token firmado
        SignedJWT jwt = new SignedJWT(header, claims);
        try {
            jwt.sign(new MACSigner(jwtConfig.getSecretKey()));
        } catch (JOSEException e) {
            throw new RuntimeException("Error al generar el token JWT", e);
        }

        return jwt.serialize();
    }
}
