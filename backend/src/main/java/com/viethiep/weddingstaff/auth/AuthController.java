package com.viethiep.weddingstaff.auth;

import com.viethiep.weddingstaff.dto.LoginRequest;
import com.viethiep.weddingstaff.dto.LoginResponse;
import com.viethiep.weddingstaff.entity.UserAccount;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import com.viethiep.weddingstaff.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userRepository;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        UserAccount user = userRepository.findByUsername(request.username()).orElseThrow();
        return new LoginResponse(
                jwtService.generateToken(user),
                user.getUsername(),
                user.getFullName(),
                user.getRole().getName().name());
    }
}
