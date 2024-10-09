package com.example.Tecmed_App.Services;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.Tecmed_App.Infra.Security.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${api.security.token.secret}")
    private String secret;
    @Autowired
    private TokenService tokenService;
    public DecodedJWT validateToken(String token) {
        try {
            System.out.println(secret);
            Algorithm algorithm = Algorithm.HMAC256(secret); // O mesmo algoritmo usado para gerar o token
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("API Tecmed.App") // O mesmo emissor usado na criação do token
                    .build();

            // Verifica e decodifica o token
            String realToken = token.replace("Bearer ", "");
            return verifier.verify(realToken);
        } catch (JWTVerificationException exception){
            // Lança uma exceção se o token for inválido
            throw new RuntimeException("Token inválido", exception);
        }
    }
}
