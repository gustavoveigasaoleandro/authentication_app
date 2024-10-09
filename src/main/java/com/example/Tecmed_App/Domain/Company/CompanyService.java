package com.example.Tecmed_App.Domain.Company;

import com.example.Tecmed_App.Domain.Company.Exception.CompanyAlreadyExistsException;
import com.example.Tecmed_App.Domain.Users.User;
import com.example.Tecmed_App.Domain.Users.UserController;
import com.example.Tecmed_App.Domain.Users.UserService;
import com.example.Tecmed_App.Services.EmailService;
import org.apache.commons.codec.DecoderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompanyService {
     @Autowired
      private CompanyRepository companyRepository;

    @Autowired
     private UserService userService;

    @Autowired
     private  EmailService emailService;

    @Value("${user.confirmation.required:true}")
    private boolean isConfirmationRequired;
     public Company AddNewCompany(Company company){
         String token = UUID.randomUUID().toString();
         if (isConfirmationRequired) {
             company.setActive(false);
             String subject = "Approval Request for New Manager";
             String body = "A new company by the name" + company.getName() + " was created. " +
                     "Please approve the company by clicking the following link: " +
                     "http://localhost:8080/api/v1/company/approve?token=" + token;
             emailService.sendEmail("veiga187@gmail.com", subject, body);
         }
         else {
             company.setActive(true);

         }

            company.setConfirmationToken(token);
            return companyRepository.save(company);
     }


    public Boolean EnableCompany(String token, User user) throws DecoderException, NoSuchAlgorithmException, InvalidKeySpecException {
        Optional<Company> optionalCompany = companyRepository.findByConfirmationToken(token);
        if (optionalCompany.isPresent() && "manager".equalsIgnoreCase(user.getRole().toString())){
            Company company = optionalCompany.get();

            user.setCompany(company);
            company.setActive(true);
            company.setConfirmationToken(null); // opcional, para invalidar o token após a confirmação
            companyRepository.save(company);
            userService.addNewUser(user);
            return true;
        }
        return false;
    }

    public void checkIfCompanyExists(String name) {
        Optional<Company> optionalCompany = companyRepository.findByName(name);
        if (optionalCompany.isPresent()) {
            throw new CompanyAlreadyExistsException("Company with name " + name + " already exists");
        }


    }
}
