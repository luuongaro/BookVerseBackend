package com.grupo3.BookVerse.features.stories.repository;

import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionEntity;
import com.grupo3.BookVerse.features.subscriptions.domain.SubscriptionType;
import com.grupo3.BookVerse.features.users.domain.UserEntity;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
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

@SpringJUnitConfig(StoryExploreRepositoryTest.Config.class)
class StoryExploreRepositoryTest {
    @Autowired EntityManagerFactory factory;
    @Autowired StoryRepository stories;

    @Test
    void filtersOwnHiddenAndDeletedStoriesBeforeCountingAndPaginating() {
        var em = factory.createEntityManager();
        em.getTransaction().begin();
        var plan = SubscriptionEntity.builder().type(SubscriptionType.FREE)
                .maxStoriesPublished(10).maxActiveStoriesReading(3).build();
        em.persist(plan);
        var reader = UserEntity.builder().username("reader").email("reader@example.test")
                .passwordHash("test-only").subscription(plan).build();
        var other = UserEntity.builder().username("other").email("other@example.test")
                .passwordHash("test-only").subscription(plan).build();
        em.persist(reader);
        em.persist(other);
        for (int i = 0; i < 8; i++) {
            var story = StoryEntity.builder().title("Story " + i).description("Description")
                    .author(i < 3 ? reader : other).build();
            em.persist(story);
            // Lifecycle defaults run on persist; apply hidden/deleted flags afterwards.
            if (i == 3) story.setHidden(true);
            if (i == 4) story.setDeleted(true);
        }
        em.getTransaction().commit();
        em.close();

        var first = stories.findByAuthorIdNotAndDeletedFalseAndHiddenFalseOrderByCreatedAtDesc(reader.getId(), PageRequest.of(0, 2));
        var last = stories.findByAuthorIdNotAndDeletedFalseAndHiddenFalseOrderByCreatedAtDesc(reader.getId(), PageRequest.of(1, 2));
        assertEquals(3, first.getTotalElements());
        assertEquals(2, first.getTotalPages());
        assertEquals(2, first.getNumberOfElements());
        assertEquals(1, last.getNumberOfElements());
        assertTrue(last.isLast());
        assertTrue(first.getContent().stream().noneMatch(item -> item.isHidden() || item.isDeleted()));
        assertTrue(first.getContent().stream().noneMatch(item -> item.getTitle().matches("Story [0-4]")));
        assertFalse(last.getContent().getFirst().getIdExternal().equals(first.getContent().getFirst().getIdExternal()));
    }

    @Configuration
    @EnableJpaRepositories(basePackageClasses = StoryRepository.class)
    static class Config {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:story-explore;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
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
