package com.grupo3.BookVerse.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mercado-pago")
@Getter
@Setter
public class MercadoPagoProperties {

    private String accessToken;
    private String webhookSecret;
}