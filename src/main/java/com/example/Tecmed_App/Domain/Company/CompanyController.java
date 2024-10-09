package com.example.Tecmed_App.Domain.Company;

import com.example.Tecmed_App.Domain.Company.Exception.CompanyNotFoundException;
import com.example.Tecmed_App.Domain.Company.Records.CompanyData;
import com.example.Tecmed_App.Domain.Users.Exception.AuthenticationException;
import com.example.Tecmed_App.Domain.Users.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.DecoderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@RestController
@RequestMapping(path = "api/v1/company")
@Tag(name = "Company Management System", description = "Operations pertaining to company in Company Management System")
public class CompanyController {

        @Autowired
        private CompanyService companyService;

        @PostMapping("save")
        public Company CreateCompany(@RequestBody Company company){
            try{
                companyService.checkIfCompanyExists(company.getName());

                throw new CompanyNotFoundException("Company with name " + company.getName() + "don't exist");
            }
            catch(CompanyNotFoundException e) {
               return companyService.AddNewCompany(company);
            }
        }

    @PostMapping("confirm")
    public ResponseEntity<String> ConfirmCompany(@RequestBody CompanyData companyData) throws DecoderException, NoSuchAlgorithmException, InvalidKeySpecException {
        User user = companyData.user();

        // Verifica se o usuário tem o papel MANAGER
        if (!"MANAGER".equalsIgnoreCase(user.getRole().name())) {
            throw new AccessDeniedException("User must have the MANAGER role to confirm the company");
        }

        boolean confirmCompany = companyService.EnableCompany(companyData.token(), user);

        if (confirmCompany) {
            return new ResponseEntity<>("Company confirmed successfully, enable the manager", HttpStatus.OK);
        } else {
            throw new AuthenticationException("Invalid token");
        }
    }

}
