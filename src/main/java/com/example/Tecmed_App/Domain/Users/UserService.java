package com.example.Tecmed_App.Domain.Users;

import com.example.Tecmed_App.Domain.Company.Company;
import com.example.Tecmed_App.Domain.Users.Exception.UserAlreadyExistsException;
import com.example.Tecmed_App.Domain.Users.Records.UserUpdateRequestDTO;
import com.example.Tecmed_App.Enums.Role;
import com.example.Tecmed_App.Services.EmailService;
import org.apache.commons.codec.DecoderException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    private final EmailService emailService;

    private final PasswordEncoder passwordEncoder;

    @Value("${user.confirmation.required}")
    private boolean isConfirmationRequired;
    @Autowired
    public UserService(UserRepository userRepository, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public User addNewUser(User user) throws NoSuchAlgorithmException, InvalidKeySpecException, DecoderException {
        // Verifica se o usuário já existe pelo email
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new UserAlreadyExistsException("User with email " + user.getEmail() + " already exists");
        }

        // Codifica a senha do usuário
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        if (isConfirmationRequired) {
            user.setActive(false);
            String token = UUID.randomUUID().toString();
            user.setConfirmationToken(token);

            if ("MANAGER".equalsIgnoreCase(user.getRole().name())) {
                String subject = "Approval Request for New Manager";
                String body = "A new user with email " + user.getEmail() + " has requested to be a manager. " +
                        "Please approve this request by clicking the following link: " +
                        "http://localhost:8080/api/v1/user/approve?token=" + token;
                emailService.sendEmail("veiga187@gmail.com", subject, body);
            } else if ("TECHNICIAN".equalsIgnoreCase(user.getRole().name())) {
                String subject = "Approval Request for New Technician";
                String body = "A new user with email " + user.getEmail() + " has requested to be a technician. " +
                        "Please approve this request by clicking the following link: " +
                        "http://localhost:8080/api/v1/user/approve?token=" + token;
                emailService.sendEmail("veiga187@gmail.com", subject, body);
            } else if ("CLIENT".equalsIgnoreCase(user.getRole().name())) {
                String subject = "Confirmação de Cadastro";
                String confirmationUrl = "http://localhost:8080/api/v1/user/confirm?token=" + token;
                String text = "Olá " + user.getUsername() + ",\n\nSeu cadastro foi realizado com sucesso. Clique no link abaixo para ativar sua conta:\n" + confirmationUrl + "\n\nObrigado!";
                emailService.sendEmail(user.getEmail(), subject, text);
            } else {
                throw new IllegalArgumentException("Invalid role");
            }
        } else {
            user.setActive(true);
        }

        return userRepository.save(user);
    }

    public User updateUser(String email, UserUpdateRequestDTO updatedData) {
        try {
            Optional<User> optionalUser = userRepository.findByEmail(email);
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();

                // Verifica se o usuário é o único gerente da empresa
                if (user.getRole() == Role.MANAGER) {
                    Page<User> managers = userRepository.findByCompanyAndRole(user.getCompany(), Role.MANAGER, PageRequest.of(0, 2));
                    if (managers.getTotalElements() == 1) {
                        throw new IllegalArgumentException("Cannot update user: the user is the only manager of the company.");
                    }
                }

                user.setUsername(updatedData.username());
                user.setRole(updatedData.role());
                return userRepository.save(user);
            } else {
                throw new IllegalArgumentException("User with email " + email + " not found");
            }
        } catch (Exception e) {
            // Re-throwing the exception to ensure it's not silently swallowed
            throw new RuntimeException("Error updating user", e);
        }
    }
    public void deleteUser(Long userId) {
        // Alterar a flag 'deleted' para true, em vez de realmente deletar o usuário
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setDeleted(true);
        userRepository.save(user);
    }

    public Page<User> getUsersByCompany(Long id, int page, int size) {

        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            Pageable pageable = PageRequest.of(page, size);
            return userRepository.findByCompany(user.getCompany(), pageable);
        }
        else {
            return Page.empty(); // Retorna uma página vazia se o usuário não for encontrado
        }
    }

    public Page<User> getUsersByCompanyAndRole(Long id, Role role, int page, int size) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            Pageable pageable = PageRequest.of(page, size);
            return userRepository.findByCompanyAndRole(user.getCompany(), role, pageable);
        }
        else {
            return Page.empty(); // Retorna uma página vazia se o usuário não for encontrado
        }
    }

    public User findUserByIdAndCompany(Long userId, Long companyId) {
        return userRepository.findByIdAndCompanyId(userId, companyId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId + " and companyId: " + companyId));
    }

    public boolean confirmUser(String token) {
        Optional<User> optionalUser = userRepository.findByConfirmationToken(token);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            user.setActive(true);
            user.setConfirmationToken(null); // opcional, para invalidar o token após a confirmação
            userRepository.save(user);
            return true;
        }
        return false;
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findUserDetailsByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
    }
}
