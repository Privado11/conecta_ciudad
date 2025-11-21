package com.unimagdalena.conectaCiudad.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import io.jsonwebtoken.Jwts;

public class TokenJwtConfig {
    public static final String PREFIX_TOKEN = "Bearer ";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String CONTENT_TYPE = "application/json";


    private static final String SECRET_STRING = "s822fe35bc9b90aaef9a631374898e3e1";

    public static final SecretKey SECRET_KEY =
            new SecretKeySpec(SECRET_STRING.getBytes(), Jwts.SIG.HS256.key().build().getAlgorithm());
}