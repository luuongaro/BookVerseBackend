package com.grupo3.BookVerse.features.users;

import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.books.domain.BookEntity;
import com.grupo3.BookVerse.features.chapters.domain.ChapterEntity;
import com.grupo3.BookVerse.features.groups.groupMember.domain.*;
import com.grupo3.BookVerse.features.groups.groupMember.repository.GroupMemberRepository;
import com.grupo3.BookVerse.features.groups.readingGroups.domain.ReadingGroupEntity;
import com.grupo3.BookVerse.features.status.domain.*;
import com.grupo3.BookVerse.features.status.repository.ReadingStatusRepository;
import com.grupo3.BookVerse.features.stories.domain.StoryEntity;
import com.grupo3.BookVerse.features.stories.repository.StoryRepository;
import com.grupo3.BookVerse.features.subscriptions.domain.*;
import com.grupo3.BookVerse.features.users.domain.*;
import com.grupo3.BookVerse.features.users.dto.ProfileAppearanceRequestDto;
import com.grupo3.BookVerse.features.users.repository.*;
import com.grupo3.BookVerse.features.users.service.UserProfileExtrasService;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringJUnitConfig(ProfileExtrasRepositoryTest.Config.class)
@Transactional
class ProfileExtrasRepositoryTest {
    @PersistenceContext EntityManager em;
    @Autowired UserRepository users;
    @Autowired StoryRepository stories;
    @Autowired ReadingStatusRepository statuses;
    @Autowired GroupMemberRepository members;
    @Autowired ProfileAppearanceRepository appearances;
    @Autowired FavoriteStoryRepository favorites;
    UserProfileExtrasService service;
    UserEntity reader;
    UserEntity other;
    List<StoryEntity> works;

    @BeforeEach void setup() {
        var plan = SubscriptionEntity.builder().type(SubscriptionType.FREE).maxStoriesPublished(10).maxActiveStoriesReading(3).build();
        em.persist(plan);
        reader = UserEntity.builder().username("reader").email("reader@example.test").passwordHash("test-only").subscription(plan).build();
        other = UserEntity.builder().username("other").email("other@example.test").passwordHash("test-only").subscription(plan).build();
        em.persist(reader); em.persist(other);
        works = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            var story = StoryEntity.builder().title("Story " + i).description("Description").author(reader).build();
            em.persist(story); works.add(story);
        }
        service = new UserProfileExtrasService(users, stories, statuses, members, appearances, favorites);
        authenticate(reader);
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    void authenticate(UserEntity user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test void aggregatesBooksStoriesRereadingAndOnlyActiveGroups() {
        var book = BookEntity.builder().googleBookId("test-book").title("Book").build(); em.persist(book);
        em.persist(ReadingStatusEntity.builder().user(reader).book(book).status(ReadingStatusEnum.WANT_TO_READ).build());
        var values = List.of(ReadingStatusEnum.WANT_TO_READ, ReadingStatusEnum.IN_PROGRESS, ReadingStatusEnum.RE_READING, ReadingStatusEnum.FINISHED, ReadingStatusEnum.ABANDONED);
        for (int i = 0; i < values.size(); i++) em.persist(ReadingStatusEntity.builder().user(reader).story(works.get(i)).status(values.get(i)).build());
        em.persist(ReadingStatusEntity.builder().user(other).story(works.getFirst()).status(ReadingStatusEnum.FINISHED).build());
        for (int i = 0; i < 3; i++) {
            var group = ReadingGroupEntity.builder().name("Group " + i).createdBy(reader).story(works.get(i)).isActive(i != 1).build();
            em.persist(group);
            em.persist(GroupMemberEntity.builder().group(group).user(reader).memberType(GroupMemberType.MEMBER)
                    .status(i == 2 ? GroupMemberStatus.LEFT : GroupMemberStatus.ACTIVE).build());
        }
        var activity = service.activity(reader.getIdExternal());
        assertEquals(2, activity.wantToRead()); assertEquals(2, activity.inProgress());
        assertEquals(1, activity.finished()); assertEquals(8, activity.createdStories()); assertEquals(1, activity.activeGroups());
    }

    @Test void onlyVisibleStoriesWithANondeletedPublishedChapterAllowProfileTips() {
        assertNull(service.activity(reader.getIdExternal()).tipStoryId());
        for (int i = 0; i < 4; i++) {
            var chapter = ChapterEntity.builder().story(works.get(i)).chapterNumber(1).title("Chapter")
                    .content("Content").published(i != 0).build();
            em.persist(chapter);
            if (i == 1) works.get(i).setHidden(true);
            if (i == 2) works.get(i).setDeleted(true);
            if (i == 3) chapter.setDeleted(true);
        }
        assertNull(service.activity(reader.getIdExternal()).tipStoryId());
        var otherChapter = ChapterEntity.builder().story(works.get(5)).chapterNumber(1).title("Other")
                .content("Content").published(true).build();
        works.get(5).setAuthor(other); em.persist(otherChapter);
        assertNull(service.activity(reader.getIdExternal()).tipStoryId());
        var published = ChapterEntity.builder().story(works.get(4)).chapterNumber(1).title("Published")
                .content("Content").published(true).build();
        em.persist(published);
        assertEquals(works.get(4).getIdExternal(), service.activity(reader.getIdExternal()).tipStoryId());
    }

    @Test void persistsCoverAndAvatarIndependentlyAndNeverChangesAnotherUser() {
        assertFalse(service.appearance(reader.getIdExternal()).configured());
        service.updateAppearance(new ProfileAppearanceRequestDto(4, null));
        service.updateAppearance(new ProfileAppearanceRequestDto(null, 3));
        em.flush(); em.clear();
        var saved = service.appearance(reader.getIdExternal());
        assertEquals(4, saved.coverIndex()); assertEquals(3, saved.avatarIndex()); assertTrue(saved.configured());
        assertFalse(service.appearance(other.getIdExternal()).configured());
        assertEquals(0, service.appearance(other.getIdExternal()).avatarIndex());
    }

    @Test void favoritesAreIdempotentAndRemovingOnlyAffectsTheAuthenticatedOwner() {
        var id = works.getFirst().getIdExternal();
        service.addFavorite(id); service.addFavorite(id);
        assertEquals(1, favorites.count()); assertTrue(service.isFavorite(id));
        authenticate(other); assertFalse(service.isFavorite(id)); service.removeFavorite(id);
        assertEquals(1, favorites.count());
        service.addFavorite(id); assertEquals(2, favorites.count());
        service.removeFavorite(id); assertEquals(1, favorites.count());
        authenticate(reader); assertTrue(service.isFavorite(id));
    }

    @Test void hiddenAndDeletedFavoritesAreFilteredBeforeCountingAndPaginating() {
        for (int i = 0; i < 4; i++) service.addFavorite(works.get(i).getIdExternal());
        works.get(2).setHidden(true); works.get(3).setDeleted(true); em.flush();
        var first = service.favorites(reader.getIdExternal(), PageRequest.of(0, 1));
        var second = service.favorites(reader.getIdExternal(), PageRequest.of(1, 1));
        assertEquals(2, first.getTotalElements()); assertEquals(2, first.getTotalPages());
        assertNotEquals(first.getContent().getFirst().idExternal(), second.getContent().getFirst().idExternal());
        assertThrows(ResourceNotFoundException.class, () -> service.addFavorite(works.get(2).getIdExternal()));
        assertThrows(ResourceNotFoundException.class, () -> service.addFavorite(works.get(3).getIdExternal()));
    }

    @Test void unavailableProfilesAndAnonymousWritesAreRejected() {
        other.setStatus(UserStatus.INACTIVE); em.flush();
        assertThrows(ResourceNotFoundException.class, () -> service.favorites(other.getIdExternal(), PageRequest.of(0, 12)));
        assertThrows(ResourceNotFoundException.class, () -> service.activity(other.getIdExternal()));
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> service.addFavorite(works.getFirst().getIdExternal()));
        assertThrows(AccessDeniedException.class, () -> service.updateAppearance(new ProfileAppearanceRequestDto(1, null)));
    }

    @Configuration
    @EnableJpaRepositories(basePackageClasses = {UserRepository.class, StoryRepository.class, ReadingStatusRepository.class, GroupMemberRepository.class})
    static class Config {
        @Bean DataSource dataSource() { return new DriverManagerDataSource("jdbc:h2:mem:profile-extras;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", ""); }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var bean = new LocalContainerEntityManagerFactoryBean(); bean.setDataSource(source);
            bean.setPackagesToScan("com.grupo3.BookVerse.features"); bean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            var properties = new Properties(); properties.setProperty("hibernate.hbm2ddl.auto", "create-drop"); bean.setJpaProperties(properties); return bean;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
    }
}
