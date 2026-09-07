package com.bank.service.impl;

import com.bank.dto.AuthResponse;
import com.bank.dto.LoginRequest;
import com.bank.dto.RegisterRequest;
import com.bank.entity.User;
import com.bank.entity.enums.UserRole;
import com.bank.exception.DuplicateEmailException;
import com.bank.exception.ResourceNotFoundException;
import com.bank.repository.UserRepository;
import com.bank.security.JwtTokenProvider;
import com.bank.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email is already registered: " + request.getEmail());
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(
                request.getName(),
                request.getEmail(),
                request.getPhone(),
                encodedPassword,
                UserRole.CUSTOMER
        );

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        String token = tokenProvider.generateToken(user.getEmail(), user.getRole());

        return new AuthResponse(
                token,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
