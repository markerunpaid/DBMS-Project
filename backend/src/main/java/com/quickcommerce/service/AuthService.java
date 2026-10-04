package com.quickcommerce.service;

import com.quickcommerce.dto.AuthDtos.AuthResponse;
import com.quickcommerce.dto.AuthDtos.LoginRequest;
import com.quickcommerce.dto.AuthDtos.RegisterRequest;
import com.quickcommerce.entity.Customer;
import com.quickcommerce.exception.ApiException;
import com.quickcommerce.repository.CustomerRepository;
import com.quickcommerce.repository.DarkStoreRepository;
import com.quickcommerce.repository.DeliveryPartnerRepository;
import com.quickcommerce.repository.EmployeeRepository;
import com.quickcommerce.security.AuthUser;
import com.quickcommerce.security.JwtService;
import com.quickcommerce.security.Role;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final CustomerRepository customers;
    private final EmployeeRepository employees;
    private final DeliveryPartnerRepository partners;
    private final DarkStoreRepository stores;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(CustomerRepository customers, EmployeeRepository employees,
                       DeliveryPartnerRepository partners, DarkStoreRepository stores,
                       PasswordEncoder encoder, JwtService jwt) {
        this.customers = customers;
        this.employees = employees;
        this.partners = partners;
        this.stores = stores;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (customers.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        if (customers.existsByPhone(req.phone())) {
            throw ApiException.conflict("An account with this phone number already exists");
        }
        Customer c = new Customer();
        c.setFirstName(req.firstName().trim());
        c.setMiddleName(blankToNull(req.middleName()));
        c.setLastName(blankToNull(req.lastName()));
        c.setPhone(req.phone());
        c.setEmail(email);
        c.setPasswordHash(encoder.encode(req.password()));
        customers.save(c);
        return respond(c.getId(), Role.CUSTOMER, c.fullName(), c.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim();
        return switch (req.role()) {
            case CUSTOMER -> customers.findByEmailIgnoreCase(email)
                    .filter(c -> encoder.matches(req.password(), c.getPasswordHash()))
                    .map(c -> respond(c.getId(), Role.CUSTOMER, c.fullName(), c.getEmail()))
                    .orElseThrow(AuthService::badCredentials);
            case ADMIN -> {
                var e = employees.findByEmailIgnoreCase(email)
                        .filter(emp -> emp.getPasswordHash() != null
                                && encoder.matches(req.password(), emp.getPasswordHash()))
                        .orElseThrow(AuthService::badCredentials);
                // authorization: only store managers get the admin panel
                if (!stores.existsByManagerEmployeeId(e.getId())) {
                    throw ApiException.forbidden("This employee does not manage any dark store");
                }
                yield respond(e.getId(), Role.ADMIN, e.fullName(), e.getEmail());
            }
            case PARTNER -> partners.findByEmailIgnoreCase(email)
                    .filter(p -> encoder.matches(req.password(), p.getPasswordHash()))
                    .map(p -> respond(p.getId(), Role.PARTNER, p.fullName(), p.getEmail()))
                    .orElseThrow(AuthService::badCredentials);
        };
    }

    private AuthResponse respond(Long id, Role role, String name, String email) {
        return new AuthResponse(jwt.issue(new AuthUser(id, role)), role, id, name, email);
    }

    private static ApiException badCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
