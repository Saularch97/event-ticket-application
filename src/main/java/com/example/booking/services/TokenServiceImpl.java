package com.example.booking.services;

import com.example.booking.controller.dto.LoginRequest;
import com.example.booking.controller.dto.LoginResponse;
import com.example.booking.domain.entities.Role;
import com.example.booking.repository.UserRepository;
import com.example.booking.services.intefaces.TokenService;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.stream.Collectors;

@Transactional
@Service
public class TokenServiceImpl implements TokenService {


    private final JwtEncoder jwtEncoder;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;

    public TokenServiceImpl(JwtEncoder jwtEncoder, UserRepository userRepository, BCryptPasswordEncoder encoder) {
        this.jwtEncoder = jwtEncoder;
        this.userRepository = userRepository;
        this.encoder = encoder;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        var user = userRepository.findByUserName(loginRequest.username());

        if (user.isEmpty() || !user.get().isLoginCorrect(loginRequest, encoder)) {
            throw new BadCredentialsException("user or password is invalid");
        }

        var expiresIn = 300L;
        var now = Instant.now();

        // TODO see how admin is being saved
        var scopes = user.get().getRoles().stream().map(Role::getName).collect(Collectors.joining(" ")).toUpperCase();

        var claims = JwtClaimsSet.builder()
                .issuer("booking")
                .subject(user.get().getUserId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expiresIn))
                .claim("scope", scopes)
                .build();

        var jwtValue = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

        return new LoginResponse(jwtValue, expiresIn);
    }
}
