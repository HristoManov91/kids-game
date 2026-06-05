package com.kidsgame.mathapp.config;

import com.kidsgame.mathapp.admin.AdminAccess;
import com.kidsgame.mathapp.auth.JwtAuthenticationFilter;
import com.kidsgame.mathapp.auth.JwtService;
import com.kidsgame.mathapp.auth.UserPrincipal;
import com.kidsgame.mathapp.reward.AdminRewardCatalogController;
import com.kidsgame.mathapp.reward.AdminRewardCatalogService;
import com.kidsgame.mathapp.reward.RewardAlbumController;
import com.kidsgame.mathapp.reward.RewardAlbumService;
import com.kidsgame.mathapp.user.Role;
import com.kidsgame.mathapp.user.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = SecurityConfigTest.TestApp.class,
        properties = {
                "app.cors.allowed-origins=http://localhost:5173",
                "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration,"
                        + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                        + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration"
        }
)
class SecurityConfigTest {
    private final WebApplicationContext context;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AdminAccess adminAccess;

    private MockMvc mockMvc;

    @Autowired
    SecurityConfigTest(
            WebApplicationContext context,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            AdminAccess adminAccess
    ) {
        this.context = context;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.adminAccess = adminAccess;
    }

    @BeforeEach
    void setUp() {
        reset(jwtService, userDetailsService, adminAccess);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void protectsRewardCatalogFromAnonymousUsers() throws Exception {
        mockMvc.perform(get("/api/rewards/catalog"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAuthenticatedUsersToReadRewardCatalog() throws Exception {
        mockMvc.perform(get("/api/rewards/catalog").with(user(principal("mila", Role.CHILD))))
                .andExpect(status().isOk());
    }

    @Test
    void blocksAdminCatalogForRegularUsers() throws Exception {
        when(adminAccess.isAdmin(any(Authentication.class))).thenReturn(false);

        mockMvc.perform(get("/api/admin/reward-catalog").with(user(principal("mila", Role.CHILD))))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminCatalogForConfiguredAdmins() throws Exception {
        when(adminAccess.isAdmin(any(Authentication.class))).thenReturn(true);

        mockMvc.perform(get("/api/admin/reward-catalog").with(user(principal("hristo", Role.PARENT))))
                .andExpect(status().isOk());
    }

    @Test
    void acceptsValidBearerTokenAndRejectsInvalidBearerToken() throws Exception {
        UserPrincipal principal = principal("mila", Role.CHILD);
        when(jwtService.validateAndReadUsername("valid-token")).thenReturn(Optional.of("mila"));
        when(jwtService.validateAndReadUsername("bad-token")).thenReturn(Optional.empty());
        when(userDetailsService.loadUserByUsername("mila")).thenReturn(principal);

        mockMvc.perform(get("/api/rewards/catalog").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/rewards/catalog").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsCorsHeadersOnlyForAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/rewards/catalog")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(options("/api/rewards/catalog")
                        .header("Origin", "https://evil.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }

    private static UserPrincipal principal(String username, Role role) {
        UserEntity user = new UserEntity(username, username, "hash", role);
        ReflectionTestUtils.setField(user, "id", 7L);
        return new UserPrincipal(user);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(TestConfig.class)
    static class TestApp {
    }

    @Configuration
    @Import({
            SecurityConfig.class,
            ApiExceptionHandler.class,
            RewardAlbumController.class,
            AdminRewardCatalogController.class
    })
    static class TestConfig {
        @Bean
        JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
            return new JwtAuthenticationFilter(jwtService, userDetailsService);
        }

        @Bean
        JwtService jwtService() {
            return mock(JwtService.class);
        }

        @Bean
        UserDetailsService userDetailsService() {
            return mock(UserDetailsService.class);
        }

        @Bean
        AdminAccess adminAccess() {
            return mock(AdminAccess.class);
        }

        @Bean
        RewardAlbumService rewardAlbumService() {
            return mock(RewardAlbumService.class);
        }

        @Bean
        AdminRewardCatalogService adminRewardCatalogService() {
            return mock(AdminRewardCatalogService.class);
        }
    }
}
