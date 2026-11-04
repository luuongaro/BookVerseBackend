package com.grupo3.BookVerse.infrastructure.notifications;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.time.Duration;

@Configuration
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendMailConfiguration {
    @Bean
    public MailDelivery resendMailDelivery(@Value("${app.mail.resend-api-key:}") String key) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        return new ResendMailDelivery(RestClient.builder().requestFactory(factory), key);
    }
}
