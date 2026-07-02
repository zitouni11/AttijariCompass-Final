package com.adem.attijari_compass.security;

import com.adem.attijari_compass.entity.Role;
import com.adem.attijari_compass.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void generatesAndReadsTokenWithLocalDevelopmentSecret() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(
                jwtService,
                "secretKey",
                "YXR0aWphcmktY29tcGFzcy1sb2NhbC1kZXZlbG9wbWVudC1qd3Qtc2VjcmV0LTIwMjY=");
        ReflectionTestUtils.setField(jwtService, "expiration", 86_400_000L);

        User user = User.builder()
                .email("user@example.com")
                .password("unused")
                .role(Role.USER)
                .active(true)
                .deleted(false)
                .build();

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo(user.getEmail());
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }
}
