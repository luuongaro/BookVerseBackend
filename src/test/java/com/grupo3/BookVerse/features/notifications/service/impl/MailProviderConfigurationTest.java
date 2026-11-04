package com.grupo3.BookVerse.features.notifications.service.impl;

import com.grupo3.BookVerse.infrastructure.notifications.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import java.io.IOException;
import java.io.UncheckedIOException;
import static org.junit.jupiter.api.Assertions.*;

class MailProviderConfigurationTest {
    ApplicationContextRunner runner(boolean render) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
                .withUserConfiguration(ResendMailConfiguration.class, SmtpMailDelivery.class, EmailServiceImpl.class)
                .withPropertyValues("MAIL_FROM=BookVerse <notificaciones@bookverse.com.ar>")
                .withInitializer(context -> {
                    // Test the checked-in defaults without inheriting real process secrets.
                    var sources = context.getEnvironment().getPropertySources();
                    sources.remove("systemEnvironment");
                    try {
                        var loader = new YamlPropertySourceLoader();
                        for (var source : loader.load("mail-base", new ClassPathResource("application.yaml"))) sources.addLast(source);
                        if (render) for (var source : loader.load("mail-render", new ClassPathResource("application-render.yaml"))) sources.addBefore("mail-base", source);
                    } catch (IOException exception) { throw new UncheckedIOException(exception); }
                });
    }

    @Test void renderDefaultsToResendWithoutRequiringSmtpEnvironmentVariables() {
        runner(true).withPropertyValues("RESEND_API_KEY=re_test_key").run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(1, context.getBeansOfType(MailDelivery.class).size());
            assertInstanceOf(ResendMailDelivery.class, context.getBean(MailDelivery.class));
            assertTrue(context.getBeansOfType(SmtpMailDelivery.class).isEmpty());
            assertNotNull(context.getBean(EmailServiceImpl.class));
        });
    }

    @Test void localDevelopmentDefaultsToSmtpWithoutAResendApiKey() {
        runner(false).run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(1, context.getBeansOfType(MailDelivery.class).size());
            assertInstanceOf(SmtpMailDelivery.class, context.getBean(MailDelivery.class));
        });
    }

    @Test void renderFailsClearlyWhenItsRequiredApiKeyIsMissing() {
        runner(true).withPropertyValues("RESEND_API_KEY=").run(context -> {
            var failure = context.getStartupFailure();
            assertNotNull(failure);
            while (failure.getCause() != null) failure = failure.getCause();
            assertTrue(failure.getMessage().contains("RESEND_API_KEY"));
        });
    }

    @Test void anExplicitProviderOverrideSelectsOnlyThatProvider() {
        runner(true).withPropertyValues("MAIL_PROVIDER=smtp").run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(1, context.getBeansOfType(MailDelivery.class).size());
            assertInstanceOf(SmtpMailDelivery.class, context.getBean(MailDelivery.class));
        });
    }
}
