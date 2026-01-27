package com.coursistant.lms.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.coursistant.lms.common.enums.ResultCodeEnum;
import com.coursistant.lms.exception.CustomException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Component
public class JwtParserUtil {

    private RSAPublicKey publicKey = null;

    @PostConstruct
    public void init() {
        try {
            publicKey = (RSAPublicKey) RsaKeyUtil.loadPublicKey("public.pem");
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key", e);
        }
    }

    public Integer getUserId(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.RSA256(publicKey, null))
                    .build()
                    .verify(token);

            Claim userIdClaim = jwt.getClaim("userId");
            if (userIdClaim != null && !userIdClaim.isNull()) {
                return userIdClaim.asInt();
            }

            String subject = jwt.getSubject();
            if (subject != null) {
                try {
                    return Integer.parseInt(subject);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            throw new CustomException(ResultCodeEnum.TOKEN_INVALID_ERROR);

        } catch (Exception e) {
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
    }

    public String getRole(String token) {
        try {
            DecodedJWT jwt = JWT.require(Algorithm.RSA256(publicKey, null))
                    .build()
                    .verify(token);

            Claim roleClaim = jwt.getClaim("role");
            if (roleClaim != null && !roleClaim.isNull()) {
                return roleClaim.asString();
            }

            List<String> audience = jwt.getAudience();
            if (audience != null && !audience.isEmpty()) {
                String[] parts = audience.getFirst().split("-");
                if (parts.length > 1) {
                    return parts[1];
                }
            }

            throw new CustomException(ResultCodeEnum.TOKEN_INVALID_ERROR);

        } catch (Exception e) {
            throw new CustomException(ResultCodeEnum.TOKEN_CHECK_ERROR);
        }
    }
}