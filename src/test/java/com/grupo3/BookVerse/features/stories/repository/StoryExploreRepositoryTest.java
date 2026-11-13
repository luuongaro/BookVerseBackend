package com.grupo3.BookVerse.features.stories.repository;

import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.chapters.domain.ChapterEntity;
import com.grupo3.BookVerse.features.chapters.repository.ChapterRepository;
import com.grupo3.BookVerse.features.friends.Friendship;
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
    @Autowired ChapterRepository chapters;

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
        var friendship = new Friendship();
        friendship.setIdExternal(java.util.UUID.randomUUID());
        friendship.setUserLow(reader);
        friendship.setUserHigh(other);
        friendship.setRequester(reader);
        friendship.setStatus(Friendship.Status.PENDING);
        friendship.setCreatedAt(java.time.LocalDateTime.now());
        friendship.setUpdatedAt(java.time.LocalDateTime.now());
        em.persist(friendship);
        for (int i = 0; i < 8; i++) {
            var story = StoryEntity.builder().title("Story " + i).description("Description").genre(i == 5 || i == 6 ? "Fantasía" : "Romance")
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
        var filtered = stories.searchExplore(reader.getId(), "story", "OTHER", "FANTASÍA", false, Friendship.Status.ACCEPTED, PageRequest.of(0, 1));
        assertEquals(2, filtered.getTotalElements());
        assertEquals(2, filtered.getTotalPages());
        assertEquals(0, stories.searchExplore(reader.getId(), "Story 3", "", "", false, Friendship.Status.ACCEPTED, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, stories.searchExplore(reader.getId(), "", "reader", "", false, Friendship.Status.ACCEPTED, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, stories.searchExplore(reader.getId(), "", "", "", true, Friendship.Status.ACCEPTED, PageRequest.of(0, 10)).getTotalElements());
        var update = factory.createEntityManager();
        update.getTransaction().begin();
        update.find(Friendship.class, friendship.getId()).setStatus(Friendship.Status.ACCEPTED);
        update.getTransaction().commit();
        update.close();
        assertEquals(2, stories.searchExplore(reader.getId(), "", "", "Fantasía", true, Friendship.Status.ACCEPTED, PageRequest.of(0, 1)).getTotalElements());
        assertEquals(java.util.Set.of("Fantasía", "Romance"), new java.util.HashSet<>(stories.findExploreGenres(reader.getId())));

        var addChapters = factory.createEntityManager();
        addChapters.getTransaction().begin();
        var persistedStory = addChapters.find(StoryEntity.class, first.getContent().getFirst().getId());
        var published = ChapterEntity.builder().story(persistedStory).chapterNumber(1).title("Publicado").content("Texto privado").published(true).build();
        var draft = ChapterEntity.builder().story(persistedStory).chapterNumber(2).title("Borrador").content("Texto privado").published(false).build();
        var deleted = ChapterEntity.builder().story(persistedStory).chapterNumber(3).title("Eliminado").content("Texto privado").published(true).build();
        addChapters.persist(published);
        addChapters.persist(draft);
        addChapters.persist(deleted);
        deleted.setDeleted(true);
        addChapters.getTransaction().commit();
        addChapters.close();
        assertEquals(3, chapters.countByStoryId(persistedStory.getId()));
        assertEquals(1, chapters.countByStoryIdAndDeletedFalseAndPublishedTrue(persistedStory.getId()));
        assertEquals(java.util.List.of("Publicado"), chapters.findSummaries(persistedStory.getId(), false).stream().map(c -> c.getTitle()).toList());
        assertEquals(java.util.List.of("Publicado", "Borrador"), chapters.findSummaries(persistedStory.getId(), true).stream().map(c -> c.getTitle()).toList());

    }

    @Configuration
    @EnableJpaRepositories(basePackageClasses = {StoryRepository.class, ChapterRepository.class})
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
