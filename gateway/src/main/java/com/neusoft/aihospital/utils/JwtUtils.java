package com.neusoft.aihospital.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

public class JwtUtils {
    private static final String SECRET="Eh7*Kp9@z#R2mX8!vQ5%tB3&wY6$sD4^fG7*jH8(kL9)lP0_oN1aB2cD3eF4g";
    private static final SecretKey KEY= Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    private static final Long EXPIRE=1000L*60*60*12;
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
