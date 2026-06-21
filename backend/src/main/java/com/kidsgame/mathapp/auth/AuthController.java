package com.kidsgame.mathapp.auth;

import com.kidsgame.mathapp.user.Role;
import com.kidsgame.mathapp.user.UserEntity;
import com.kidsgame.mathapp.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            PasswordResetService passwordResetService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticate(request);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return new AuthResponse(jwtService.createToken(principal), UserResponse.from(principal));
    }

    private org.springframework.security.core.Authentication authenticate(LoginRequest request) {
        try {
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Грешен акаунт или парола.");
        }
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        if (!request.password().equals(request.repeatPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Паролите не съвпадат.");
        }
        String username = request.username().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Акаунтът вече съществува.");
        }
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email адресът вече се използва.");
        }

        UserEntity user = new UserEntity(
                username,
                username,
                passwordEncoder.encode(request.password()),
                Role.CHILD
        );
        user.updateEmail(email);
        UserPrincipal principal = new UserPrincipal(userRepository.save(user));
        return new AuthResponse(jwtService.createToken(principal), UserResponse.from(principal));
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return new MessageResponse("Ако има профил с този email, изпратихме инструкции за нова парола.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.password().equals(request.repeatPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Паролите не съвпадат.");
        }
        passwordResetService.resetPassword(request.token(), request.password());
        return new MessageResponse("Паролата е сменена. Вече можеш да влезеш.");
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return UserResponse.from(principal);
    }

    @PutMapping("/email")
    public AccountEmailResponse updateEmail(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateEmailRequest request
    ) {
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        userRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(principal.id()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email адресът вече се използва.");
                });
        UserEntity user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Профилът не е намерен."));
        user.updateEmail(email);
        UserEntity saved = userRepository.save(user);
        return new AccountEmailResponse(saved.getEmail());
    }

    @GetMapping("/email")
    public AccountEmailResponse email(@AuthenticationPrincipal UserPrincipal principal) {
        UserEntity user = userRepository.findById(principal.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Профилът не е намерен."));
        return new AccountEmailResponse(user.getEmail());
    }

    @PostMapping("/children")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createChild(@Valid @RequestBody CreateChildRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Потребителското име вече съществува.");
        }

        UserEntity child = new UserEntity(
                username,
                request.displayName().trim(),
                passwordEncoder.encode(request.password()),
                Role.CHILD
        );
        UserEntity saved = userRepository.save(child);
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getDisplayName(), saved.getRole());
    }
}
