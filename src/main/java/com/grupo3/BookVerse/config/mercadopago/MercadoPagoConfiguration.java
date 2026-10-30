package com.grupo3.BookVerse.config.mercadopago;

import com.grupo3.BookVerse.config.properties.MercadoPagoProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MercadoPagoConfiguration {

    public MercadoPagoConfiguration(MercadoPagoProperties properties) {
        com.mercadopago.MercadoPagoConfig.setAccessToken(
                properties.getAccessToken()
        );
    }
}