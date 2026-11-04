package com.grupo3.BookVerse.auth;

import com.grupo3.BookVerse.auth.config.PasswordRecoveryConfig;
import com.grupo3.BookVerse.auth.repository.PasswordResetTokenRepository;
import com.grupo3.BookVerse.auth.service.PasswordRecoveryService;
import com.grupo3.BookVerse.features.subscriptions.domain.*;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import com.grupo3.BookVerse.infrastructure.EmailService;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.util.concurrent.Executor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig(PasswordRecoveryAsyncTest.Config.class)
class PasswordRecoveryAsyncTest {
    @PersistenceContext EntityManager em;
    @Autowired PlatformTransactionManager manager;
    @Autowired PasswordRecoveryService recovery;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired EmailService emails;

    @Test void requestReturnsWithoutWaitingForTheAccountLockAndMailWaitsForCommit() {
        var tx = new TransactionTemplate(manager);
        Long id = tx.execute(status -> {
            var plan = SubscriptionEntity.builder().type(SubscriptionType.FREE)
                    .maxStoriesPublished(10).maxActiveStoriesReading(3).build();
            em.persist(plan);
            var user = UserEntity.builder().username("AsyncReader").email("async@example.com")
                    .passwordHash("unused-test-hash").subscription(plan).build();
            em.persist(user);
            return user.getId();
        });
        tx.executeWithoutResult(status -> {
            users.findForPasswordRecoveryById(id).orElseThrow();
            recovery.requestReset("async@example.com");
            // The worker cannot issue a token until this transaction releases
            // the user row, but the caller has already received control back.
            assertEquals(0, tokens.count());
            verifyNoInteractions(emails);
        });
        verify(emails, timeout(5000)).sendPasswordResetEmail(eq("async@example.com"), anyString(), eq(30L));
        assertEquals(1, tokens.count());
    }

    @Configuration
    @EnableAsync
    @Import(PasswordRecoveryTest.Config.class)
    static class Config {
        @Bean @Primary DataSource asyncSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:password-recovery-async;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean(name = "passwordRecoveryTaskExecutor") Executor executor() {
            return new PasswordRecoveryConfig().passwordRecoveryTaskExecutor();
        }
    }
}
