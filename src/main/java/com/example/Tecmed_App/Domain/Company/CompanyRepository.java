package com.example.Tecmed_App.Domain.Company;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    Optional<Company> findByConfirmationToken(String token);

    Optional<Company> findByName(String name);

}
