package com.grupo3.BookVerse.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@ConfigurationProperties(prefix = "bookverse.premium")
@Getter
@Setter
public class PremiumSubscriptionProperties {

    private BigDecimal price;
    private String backUrl;
    private String testPayerEmail;
}