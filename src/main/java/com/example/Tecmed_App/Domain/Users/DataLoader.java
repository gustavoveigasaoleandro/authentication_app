package com.example.Tecmed_App.Domain.Users;

import com.example.Tecmed_App.Domain.Users.User;
import com.example.Tecmed_App.Domain.Users.UserRepository;
import com.example.Tecmed_App.Enums.Role;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.boot.CommandLineRunner;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    // Lê o e-mail e senha do administrador a partir das variáveis de ambiente
    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;
    @Override
    public void run(String... args) throws Exception {
        // Verifica se o administrador já existe
        if (!userRepository.existsByEmail(adminEmail)) {
            String encodedPassword = passwordEncoder.encode(adminPassword);
            User admin = new User("veiga187@hotmail.com", "admin", encodedPassword, Role.ADMIN);
            admin.setActive(true); // Se necessário, ajuste outros campos depois de usar o construtor

            userRepository.save(admin);
        }
    }
}