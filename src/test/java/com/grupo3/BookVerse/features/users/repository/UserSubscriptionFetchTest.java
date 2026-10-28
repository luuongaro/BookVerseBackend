package com.grupo3.BookVerse.features.users.repository;

import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.roles.domain.RoleEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringJUnitConfig(UserSubscriptionFetchTest.Config.class)
class UserSubscriptionFetchTest {
    @Autowired EntityManagerFactory factory;
    @Autowired UserRepository users;

    @Test
    void authenticatedPrincipalKeepsItsSubscriptionAndRolesAfterTheSessionCloses() {
        var em = factory.createEntityManager();
        em.getTransaction().begin();
        var subscription = SubscriptionEntity.builder().type(SubscriptionType.FREE)
                .maxStoriesPublished(2).maxActiveStoriesReading(3).build();
        var role = RoleEntity.builder().name("ROLE_USER").build();
        em.persist(subscription);
        em.persist(role);
        var user = UserEntity.builder().username("reader").email("reader@example.test")
                .passwordHash("test-only").subscription(subscription).roles(Set.of(role)).build();
        em.persist(user);
        em.getTransaction().commit();
        em.close();

        // The authentication query returns a detached principal, as it does between requests.
        var principal = users.findByEmailWithRoles("READER@EXAMPLE.TEST").orElseThrow();
        assertDoesNotThrow(() -> {
            assertEquals(SubscriptionType.FREE, principal.getSubscription().getType());
            assertEquals(3, principal.getSubscription().getMaxActiveStoriesReading());
            assertEquals("ROLE_USER", principal.getAuthorities().iterator().next().getAuthority());
        });
    }

    @Configuration
    @EnableJpaRepositories(basePackageClasses = UserRepository.class)
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:subscription-fetch;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var bean = new LocalContainerEntityManagerFactoryBean();
            bean.setDataSource(dataSource);
            bean.setPackagesToScan("com.grupo3.BookVerse.features");
            bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            var properties = new Properties();
            properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            bean.setJpaProperties(properties);
            return bean;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
    }
}
