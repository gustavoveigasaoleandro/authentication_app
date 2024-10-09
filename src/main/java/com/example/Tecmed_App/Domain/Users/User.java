package com.example.Tecmed_App.Domain.Users;

import com.example.Tecmed_App.Domain.Company.Company;
import com.example.Tecmed_App.Domain.Users.Exception.UserValidationException;
import com.example.Tecmed_App.Enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.codec.DecoderException;
import org.hibernate.annotations.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collection;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE table_product SET deleted = true WHERE id=?")
@SQLRestriction("deleted=false")
public class User implements UserDetails {
    @Id
    @SequenceGenerator(name = "user_generator", sequenceName = "user_sequence", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_sequence")
    private Long id;

    @NotNull
    @Column(unique = true)
    private String email;

    @NotNull
    private String username;

    @NotNull
    private String password;

    @NotNull
    private boolean active = false;

    private String confirmationToken;

    @NotNull
    private boolean deleted = Boolean.FALSE;

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = true)
    private Company company;

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    @Enumerated(EnumType.STRING)
    private Role role;

    public User() {
    }

    public User(String email, String username, String password, Role role) throws NoSuchAlgorithmException, InvalidKeySpecException, DecoderException {
        this.email = email;
        this.username = username;
        this.password = password;
        this.active = false;
        this.confirmationToken = generateConfirmationToken();
        this.role = role;
    }



    private String generateConfirmationToken() {
        return UUID.randomUUID().toString();
    }



    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }



    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    // UserDetails methods
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    @PrePersist
    @PreUpdate
    private void validate() {
        if (this.email == null || this.username == null || this.password == null || this.role == null) {
            throw new UserValidationException("Required fields are missing");
        }
    }
}