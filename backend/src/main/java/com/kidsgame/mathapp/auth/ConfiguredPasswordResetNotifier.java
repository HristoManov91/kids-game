package com.kidsgame.mathapp.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class ConfiguredPasswordResetNotifier implements PasswordResetNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfiguredPasswordResetNotifier.class);

    private final JavaMailSender mailSender;
    private final String delivery;
    private final String from;

    public ConfiguredPasswordResetNotifier(
            JavaMailSender mailSender,
            @Value("${app.security.password-reset.delivery}") String delivery,
            @Value("${app.security.password-reset.from}") String from
    ) {
        this.mailSender = mailSender;
        this.delivery = delivery;
        this.from = from;
    }

    @Override
    public void send(String email, String resetUrl) {
        if ("log".equalsIgnoreCase(delivery)) {
            LOGGER.info("Password reset link for {}: {}", email, resetUrl);
            return;
        }
        if (!"smtp".equalsIgnoreCase(delivery)) {
            throw new IllegalStateException("APP_PASSWORD_RESET_DELIVERY must be 'log' or 'smtp'");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("Нова парола за Kids Game");
        message.setText("Отвори този линк до 30 минути, за да зададеш нова парола:\n\n" + resetUrl
                + "\n\nАко не си поискал промяната, игнорирай това съобщение.");
        mailSender.send(message);
    }
}
