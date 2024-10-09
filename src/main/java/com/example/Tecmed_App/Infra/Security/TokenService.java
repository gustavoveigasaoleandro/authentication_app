package com.example.Tecmed_App.Infra.Security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.Tecmed_App.Domain.Users.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user) {
        try {
            var algoritmo = Algorithm.HMAC256(secret);

            return JWT.create()
                    .withIssuer("API Tecmed.App")
                    .withSubject(user.getId().toString())
                    .withClaim("role", user.getAuthorities().iterator().next().getAuthority())
                    .withClaim("companie", user.getCompany() != null && user.getCompany().getId() != null ? user.getCompany().getId() : 0)
                    .withExpiresAt(ExpirationDate())
                    .sign(algoritmo);
        } catch (JWTCreationException exception){
            throw new RuntimeException("error while generating token", exception);
        }
    }

    private Instant ExpirationDate() {
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }

    public String getSubject(String tokenJWT) {
        try {
            var algoritmo = Algorithm.HMAC256(secret);
            return JWT.require(algoritmo)
                    .withIssuer("API Tecmed.App")
                    .build()
                    .verify(tokenJWT)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            throw new RuntimeException("invalid Token!");
        }
    }


    public String getClaimFromToken(String token, String claim) {
        DecodedJWT jwt = JWT.decode(token); // Decodifica o token JWT
        return jwt.getClaim(claim).asString(); // Extrai o valor do campo (claim) especificado
    }

}
