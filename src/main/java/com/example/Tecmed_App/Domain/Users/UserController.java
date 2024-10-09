package com.example.Tecmed_App.Domain.Users;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.Tecmed_App.Domain.Company.Company;
import com.example.Tecmed_App.Domain.Company.CompanyRepository;
import com.example.Tecmed_App.Domain.Users.Exception.AuthenticationException;

import com.example.Tecmed_App.Domain.Users.Records.UserDTO;
import com.example.Tecmed_App.Domain.Users.Records.UserUpdateRequestDTO;
import com.example.Tecmed_App.Enums.Role;
import com.example.Tecmed_App.Infra.Security.JwtTokenData;
import com.example.Tecmed_App.Infra.Security.TokenService;
import com.example.Tecmed_App.Services.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.DecoderException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@RestController
@RequestMapping(path = "api/v1/user")
@Tag(name = "User Management System", description = "Operations pertaining to user in User Management System")
public class UserController {
    private final UserService userService;

    private final AuthenticationManager authenticationManager;

    @Autowired
    private final TokenService tokenService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private final CompanyRepository companyRepository;
    @Autowired
    public UserController(UserService userService, AuthenticationManager authenticationManager, TokenService tokenService, CompanyRepository companyRepository) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.companyRepository = companyRepository;
    }


    // Endpoint para criação de TECHNICIAN, acessível apenas por MANAGER

    @PostMapping("/register-user")
    public ResponseEntity<?> registerUser(@RequestBody UserDTO userDTO,
                                          @RequestParam Role role,
                                          @RequestHeader("Authorization") String authorizationHeader)
            throws NoSuchAlgorithmException, InvalidKeySpecException, DecoderException {
        try {
            // Extrair o token do cabeçalho "Authorization"
            DecodedJWT token = jwtService.validateToken(authorizationHeader);

            // Decodificar o token para extrair o campo "companie" usando a Auth0 JWT
            Integer companyId = token.getClaim("companie").asInt();

            // Buscar a empresa com base no "companie" do token
            Company company = companyRepository.findById(Long.parseLong(String.valueOf(companyId)))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid company ID"));

            // Criar o novo usuário com as informações fornecidas e a empresa extraída do token
            User user = new User(userDTO.email(), userDTO.username(), userDTO.password(), role);
            user.setCompany(company);

            // Validar o papel do usuário
            if (!role.toString().equalsIgnoreCase(user.getRole().name())) {
                throw new IllegalArgumentException("Role must match the user's role");
            }

            // Verificar se o role é válido e a empresa está presente
            if (!"MANAGER".equalsIgnoreCase(role.toString()) && !"TECHNICIAN".equalsIgnoreCase(role.toString())) {
                throw new IllegalArgumentException("Role must be MANAGER or TECHNICIAN");
            }

            if ((role.toString().equalsIgnoreCase("MANAGER") || role.toString().equalsIgnoreCase("TECHNICIAN")) && user.getCompany() == null) {
                throw new IllegalArgumentException("MANAGER and TECHNICIAN must have a company assigned");
            }

            // Registrar o usuário e retornar o resultado
            return ResponseEntity.ok(userService.addNewUser(user));
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }// Endpoint público para criação de CLIENT
    @PostMapping("/register-client")
    public ResponseEntity<User> registerClient(@RequestBody User user) throws NoSuchAlgorithmException, InvalidKeySpecException, DecoderException {
        if (!"CLIENT".equalsIgnoreCase(user.getRole().name())) {
            throw new IllegalArgumentException("Role must be CLIENT");
        }
        return ResponseEntity.ok(userService.addNewUser(user));
    }

    @PostMapping("/authenticate")
    @Operation(summary = "Authenticate a user", description = "Authenticate a user in the system")
    public ResponseEntity<JwtTokenData > authenticateUser(@RequestBody User user) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());
        Authentication authentication = authenticationManager.authenticate(authToken);
        String tokenJWT = tokenService.generateToken((User) authentication.getPrincipal());
        return ResponseEntity.ok(new JwtTokenData(tokenJWT));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser(@RequestBody User user) {
        try {
            // Autenticar o usuário com email e senha fornecidos
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());
            Authentication authentication = authenticationManager.authenticate(authToken);

            // Deletar a conta do usuário autenticado
            User authenticatedUser = (User) authentication.getPrincipal();
            userService.deleteUser(authenticatedUser.getId());
            return new ResponseEntity<>("User deleted successfully", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid email or password", HttpStatus.UNAUTHORIZED);
        }
    }
    @GetMapping("/confirm")
    public ResponseEntity<String> confirmUser(@RequestParam("token") String token) {
        boolean confirmed = userService.confirmUser(token);
        if (confirmed) {
            return new ResponseEntity<>("User confirmed successfully", HttpStatus.OK);
        } else {
            throw new AuthenticationException("Invalid token");
        }
    }

    @PostMapping("/updateData")
    public ResponseEntity<?> updateData(@RequestHeader("Authorization") String authorizationHeader, @RequestBody UserUpdateRequestDTO user) {
        String token = authorizationHeader.replace("Bearer ", "");
        try {
            // Validar o token
            // Obter o email do token
            String userId = tokenService.getSubject(token);

            User validUser = userService.findUserById(Long.parseLong(userId));

            User updatedUser = userService.findUserByIdAndCompany(user.id(),validUser.getCompany().getId());

            updatedUser = userService.updateUser(updatedUser.getEmail(), user);

            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (UsernameNotFoundException e) {
            // Se o usuário não for encontrado, captura a exceção
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
            // Faça algo caso o usuário não tenha sido encontrado
        }
        catch (Exception e) {
            return new ResponseEntity<>("An error occurred while updating user data", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Page<User>> getUsersByRole(
            @RequestParam(required = false) Role role,
            @RequestParam int page,
            @RequestParam int size,
            @RequestHeader("Authorization") String authorizationHeader) {

        // Extrai o token do cabeçalho Authorization
        String token = authorizationHeader.replace("Bearer ", "");

        // Valida o token

        // Obter o email do token
        String userId = tokenService.getSubject(token);



        Page<User> users;
        if (role == null) {
            // Use o vínculo da empresa do usuário autenticado para filtrar os usuários retornados
            users = userService.getUsersByCompany(Long.parseLong(userId) , page, size);
        } else {
            // Filtra por Role e por Company
            users = userService.getUsersByCompanyAndRole(Long.parseLong(userId), role, page, size);
        }
        return ResponseEntity.ok(users);
    }

    @GetMapping("/approve")
    @Operation(summary = "Approve manager request", description = "Approve a manager request using a token")
    public ResponseEntity<String> approveManager(@RequestParam("token") String token) {
        boolean approved = userService.confirmUser(token);
        if (approved) {
            return new ResponseEntity<>("Manager approved successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Invalid token", HttpStatus.BAD_REQUEST);
        }
    }
}
