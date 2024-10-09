package com.example.Tecmed_App.Domain.Users;

import com.example.Tecmed_App.Domain.Company.Company;
import com.example.Tecmed_App.Enums.Role;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<UserDetails> findUserDetailsByEmail(String email);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findByConfirmationToken(String token);

    Optional<User> findByIdAndCompanyId(Long userId, Long companyId);
    Page<User> findByCompany(Company company, Pageable pageable);
    @Query("SELECT u FROM User u WHERE u.company = :company AND u.role = :role")
    Page<User> findByCompanyAndRole(Company company, Role role, Pageable pageable);

}
