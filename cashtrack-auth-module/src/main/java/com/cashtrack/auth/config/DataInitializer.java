package com.cashtrack.auth.config;

import com.cashtrack.auth.entity.Role;
import com.cashtrack.auth.entity.User;
import com.cashtrack.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            createUser("admin", "admin123", Set.of(Role.BANK_ADMIN));
            createUser("customer1", "pass123", Set.of(Role.CUSTOMER));
            createUser("atm1", "atm_secret", Set.of(Role.ATM_MACHINE));
            createUser("auditor1", "audit123", Set.of(Role.AUDITOR));
        }
    }

    private void createUser(String username, String password, Set<Role> roles) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(roles);
        userRepository.save(user);
    }
}
