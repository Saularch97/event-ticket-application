package com.example.booking.config;

import com.example.booking.entities.Role;
import com.example.booking.entities.User;
import com.example.booking.repository.RoleRepository;
import com.example.booking.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Set;

@Configuration
public class AdminUserConfig implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;


    public AdminUserConfig(RoleRepository roleRepository, UserRepository userRepository, BCryptPasswordEncoder encoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.encoder = encoder;
    }


    @Override
    @Transactional
    public void run(String... args) throws Exception {
        var roleAdmin = roleRepository.findByName(Role.Values.ADMIN.name().toLowerCase());

        var userAdmin = userRepository.findByUserName("admin");

        // TODO verifify why i need to use upper case in the scopes
        userAdmin.ifPresentOrElse(
                (user) -> {
                    System.out.println("admin already exists");
                },
                () -> {
                    var user = new User();
                    user.setUserName("admin");
                    user.setPassword(encoder.encode("123"));
                    user.setRoles(Set.of(roleAdmin));
                    userRepository.save(user);
                }
        );
    }
}
