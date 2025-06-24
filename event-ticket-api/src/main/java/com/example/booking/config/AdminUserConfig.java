package com.example.booking.config;

import com.example.booking.domain.entities.User;
import com.example.booking.domain.enums.ERole;
import com.example.booking.repository.RoleRepository;
import com.example.booking.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class AdminUserConfig implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminUserConfig(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder encoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = encoder;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("HAHAHAH");
    }
    /*
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        var roleAdmin = roleRepository.findByName(ERole.ROLE_ADMIN);

        var userAdmin = userRepository.findByUserName("admin");

        userAdmin.ifPresentOrElse(
                (user) -> {
                    System.out.println("admin already exists");
                },
                () -> {
                    String senhaCodificada = passwordEncoder.encode("123");
                    System.out.println("SENHA CODIFICADA PARA O ADMIN: " + senhaCodificada); // <-- ADICIONE ISSO

                    var user = new User();
                    user.setUserName("admin");
                    user.setEmail("admin@admin.com");
                    user.setPassword(senhaCodificada); // Usa a variável
                    user.setRoles(Set.of(roleAdmin.get()));
                    userRepository.save(user);
                }
        );
    }

     */
}
