package com.example.Tecmed_App.Config.amqp;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.Tecmed_App.Class.SecurityToken;
import com.example.Tecmed_App.Domain.Users.User;
import com.example.Tecmed_App.Domain.Users.UserService;
import com.example.Tecmed_App.Services.JwtService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AMQPListener {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserService userService;
    @RabbitListener(queues = "authorization.validation")
    public void receiveMessage(@Payload SecurityToken item, Message message) {
        String token = item.getToken();
        String correlationId = message.getMessageProperties().getCorrelationId();
        String replyTo = message.getMessageProperties().getReplyTo();
        String routingKey = (String) message.getMessageProperties().getHeaders().get("customRoutingKey");

        try {
            // Validar o token JWT
            DecodedJWT decodedJWT = jwtService.validateToken(token);

            // Se o token for válido, você pode acessar as informações dentro dele
            String userId = decodedJWT.getSubject();
            String userRole = decodedJWT.getClaim("role").asString();
            Integer companyId = decodedJWT.getClaim("companie").asInt();
            System.out.println("Token válido: " + token);
            System.out.println("Usuário: " + userId);
            System.out.println("Role: " + userRole);
            System.out.println("companyId: " + companyId);

            Map<String, Object> response = new HashMap<>();
            System.out.println("replyTo: "+ replyTo);
            response.put("token", token);
            response.put("valid", true);
            response.put("userId",userId);
            response.put("role", userRole);
            response.put("companyId", companyId);
            // Envia a resposta com o correlationId e replyTo
            rabbitTemplate.convertAndSend(replyTo, routingKey, response, msg -> {
                msg.getMessageProperties().setCorrelationId(correlationId);
                return msg;
            });
        } catch (RuntimeException e) {

            Map<String, Object> response = new HashMap<>();
           
            response.put("valid", false);
            response.put("role", null);

            // Envia a resposta de erro com o correlationId e replyTo
            rabbitTemplate.convertAndSend(replyTo, routingKey, response, msg -> {
                msg.getMessageProperties().setCorrelationId(correlationId);
                return msg;
            });

            System.out.println("Token inválido ou expirado: " + token);
        }
    }

    @RabbitListener(queues = "verification.service_order")
    public void verifyServiceOrder(@Payload Map<String, Object> payload, Message message) {
        Integer clientId = (Integer) payload.get("client_id");
        String correlationId = message.getMessageProperties().getCorrelationId();
        String replyTo = message.getMessageProperties().getReplyTo();
        String routingKey = (String) message.getMessageProperties().getHeaders().get("customRoutingKey");

        Map<String, Object> response = new HashMap<>();

        try {
            // Busca o usuário pelo client_id
            User user = this.userService.findUserById(Long.valueOf(clientId));

            if (user != null) {
                // Se o usuário for encontrado, retorna o role do usuário
                response.put("client_id", clientId);
                response.put("role", user.getRole());
            } else {
                // Se o usuário não for encontrado, retorna client_id como null
                response.put("client_id", null);
                response.put("role", null);
            }

            // Envia a resposta com o correlationId e replyTo
            rabbitTemplate.convertAndSend(replyTo, routingKey, response, msg -> {
                msg.getMessageProperties().setCorrelationId(correlationId);
                return msg;
            });
        } catch (RuntimeException e) {
            // Em caso de erro, envia uma resposta de erro
            response.put("client_id", null);
            response.put("role", null);

            rabbitTemplate.convertAndSend(replyTo, routingKey, response, msg -> {
                msg.getMessageProperties().setCorrelationId(correlationId);
                return msg;
            });

            System.out.println("Erro ao buscar usuário com client_id: " + clientId);
        }
    }
}
