package com.grupo3.BookVerse.features.users;

import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import com.grupo3.BookVerse.features.users.domain.UserStatus;
import com.grupo3.BookVerse.features.users.repository.UserRepository;
import com.grupo3.BookVerse.features.users.service.UserProfileService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(UserProfileRepositoryTest.Config.class)
class UserProfileRepositoryTest {
    @Autowired EntityManagerFactory factory;
    @Autowired UserRepository users;
    @Autowired StoryRepository stories;

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }

    @Test
    void searchesActiveUsernamesBeforePaginationAndHidesUnavailableProfilesAndStories() {
        var em = factory.createEntityManager();
        em.getTransaction().begin();
        var plan = SubscriptionEntity.builder().type(SubscriptionType.FREE)
                .maxStoriesPublished(10).maxActiveStoriesReading(3).build();
        em.persist(plan);
        var sarah = user("Sarah", plan);
        var notes = user("sarah_notes", plan);
        var river = user("River", plan);
        var inactive = user("SarahInactive", plan);
        var suspended = user("SarahSuspended", plan);
        var banned = user("SarahBanned", plan);
        for (var user : new UserEntity[]{sarah, notes, river, inactive, suspended, banned}) em.persist(user);
        inactive.setStatus(UserStatus.INACTIVE);
        suspended.setStatus(UserStatus.SUSPENDED);
        banned.setStatus(UserStatus.BANNED);
        for (int i = 0; i < 5; i++) {
            var story = StoryEntity.builder().title("Story " + i).description("Description")
                    .author(i == 4 ? river : sarah).build();
            em.persist(story);
            if (i == 2) story.setHidden(true);
            if (i == 3) story.setDeleted(true);
        }
        em.getTransaction().commit();
        em.close();

        var service = new UserProfileService(users, stories);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(river, null, river.getAuthorities()));
        var first = service.search(" @sArAh ", PageRequest.of(0, 1));
        var second = service.search("sarah", PageRequest.of(1, 1));
        assertEquals(2, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals("Sarah", first.getContent().getFirst().username());
        assertEquals("sarah_notes", second.getContent().getFirst().username());
        assertEquals(2, service.search("", PageRequest.of(0, 1)).getTotalElements());
        assertTrue(service.search("@River", PageRequest.of(0, 12)).isEmpty());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(sarah, null, sarah.getAuthorities()));
        var withoutSelf = service.search("sarah", PageRequest.of(0, 1));
        assertEquals(1, withoutSelf.getTotalElements());
        assertEquals(1, withoutSelf.getTotalPages());
        assertEquals("sarah_notes", withoutSelf.getContent().getFirst().username());
        assertTrue(service.search("sarah", PageRequest.of(1, 1)).isEmpty());
        assertEquals(0, service.search("%", PageRequest.of(0, 12)).getTotalElements());
        assertEquals("Sarah", service.getProfile(sarah.getIdExternal()).username());
        for (var unavailable : new UserEntity[]{inactive, suspended, banned}) {
            assertThrows(ResourceNotFoundException.class, () -> service.getProfile(unavailable.getIdExternal()));
            assertThrows(ResourceNotFoundException.class, () -> service.getStories(unavailable.getIdExternal(), PageRequest.of(0, 1)));
        }
        var visibleStories = service.getStories(sarah.getIdExternal(), PageRequest.of(0, 1));
        var lastStories = service.getStories(sarah.getIdExternal(), PageRequest.of(1, 1));
        assertEquals(2, visibleStories.getTotalElements());
        assertEquals(2, visibleStories.getTotalPages());
        assertTrue(visibleStories.getContent().getFirst().title().matches("Story [01]"));
        assertNotEquals(visibleStories.getContent().getFirst().idExternal(), lastStories.getContent().getFirst().idExternal());
    }

    @Test void searchRequiresAnAuthenticatedPrincipal() {
        SecurityContextHolder.clearContext();
        var service = new UserProfileService(users, stories);
        assertThrows(AccessDeniedException.class, () -> service.search("Sarah", PageRequest.of(0, 12)));
    }

    private UserEntity user(String username, SubscriptionEntity plan) {
        return UserEntity.builder().username(username).email(username + "@example.test")
                .passwordHash("test-only").subscription(plan).build();
    }

    @Configuration
    @EnableJpaRepositories(basePackageClasses = {UserRepository.class, StoryRepository.class})
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:community-profiles;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var bean = new LocalContainerEntityManagerFactoryBean();
            bean.setDataSource(source);
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
