package org.example.backendp2p4.security;

import com.nimbusds.jwt.SignedJWT;

public class JwtUtil {
    public static String extraerNombreDesdeToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringClaim("name");
        } catch (Exception e) {
            throw new RuntimeException("Token inválido", e);
        }
    }
}
