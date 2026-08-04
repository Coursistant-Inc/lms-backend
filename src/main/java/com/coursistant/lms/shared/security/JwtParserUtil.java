package com.coursistant.lms.shared.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPublicKey;

@Component
public class JwtParserUtil {

    private RSAPublicKey publicKey = null;

    @Value("${token.public-key-path:public.pem}")
    private String publicKeyPath;

    @Value("${auth.jwt.issuer:https://usc.xlearnedu.com}")
    private String issuer;

    @Value("${auth.jwt.audience:com.coursistant.lms}")
    private String audience;

    @PostConstruct
    public void init() {
        try {
            publicKey = (RSAPublicKey) RsaKeyUtil.loadPublicKey(publicKeyPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key from: " + publicKeyPath, e);
        }
    }

    /**
     * Verify signature, exp, issuer, audience with RSA256 only.
     */
    public DecodedJWT verify(String token) {
        try {
            return JWT.require(Algorithm.RSA256(publicKey, null))
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .build()
                    .verify(token);
        } catch (Exception e) {
            throw new ApiException(ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }
    }
}
