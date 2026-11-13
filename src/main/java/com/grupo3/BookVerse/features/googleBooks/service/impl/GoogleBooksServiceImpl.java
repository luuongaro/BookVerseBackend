package com.grupo3.BookVerse.features.googleBooks.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.googleBooks.client.GoogleBooksClient;
import com.grupo3.BookVerse.features.googleBooks.dto.GoogleBookVolumeDto;
import com.grupo3.BookVerse.features.googleBooks.dto.GoogleBooksApiResponseDto;
import com.grupo3.BookVerse.features.googleBooks.service.GoogleBooksService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleBooksServiceImpl implements GoogleBooksService {

    private final GoogleBooksClient googleBooksClient;
    private static final Set<String> ALLOWED_LANGUAGES =
            Set.of("es", "en");

    @Override
    public GoogleBooksApiResponseDto searchBooks(String query) {
        validateSearchValue(query, "Query");

        String normalizedQuery = query.trim();
        logGoogleBooksSearch("general", normalizedQuery);

        GoogleBooksApiResponseDto responseDto =
                googleBooksClient.searchBooks(normalizedQuery);

        return filterAllowedLanguages(responseDto);
    }

    @Override
    public GoogleBooksApiResponseDto searchBooksByTitle(String title) {
        validateSearchValue(title, "Title");

        String normalizedTitle = "intitle:" + title.trim();
        logGoogleBooksSearch("title", normalizedTitle);

        GoogleBooksApiResponseDto responseDto =
                googleBooksClient.searchBooks(normalizedTitle);

        return filterAllowedLanguages(responseDto);
    }

    @Override
    public GoogleBooksApiResponseDto searchBooksByAuthor(String author) {
        validateSearchValue(author, "Author");

        String normalizedAuthor = "inauthor:" + author.trim();
        logGoogleBooksSearch("author", normalizedAuthor);

        GoogleBooksApiResponseDto responseDto =
                googleBooksClient.searchBooks(normalizedAuthor);

        return filterAllowedLanguages(responseDto);
    }

    @Override
    public GoogleBookVolumeDto getBookByGoogleId(String googleBookId) {
        validateSearchValue(
                googleBookId,
                "Google Book ID"
        );

        GoogleBookVolumeDto book =
                googleBooksClient.getBookByGoogleId(googleBookId);

        if (book == null || book.getVolumeInfo() == null) {
            throw new ResourceNotFoundException(
                    "Book not found in Google Books with id: "
                            + googleBookId
            );
        }

        String language =
                book.getVolumeInfo().getLanguage();

        if (language == null
                || !ALLOWED_LANGUAGES.contains(
                language.trim().toLowerCase()
        )) {
            throw new BadRequestException(
                    "Book language must be Spanish or English"
            );
        }

        return book;
    }

    private void validateSearchValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BadRequestException(fieldName + " parameter cannot be null or empty");
        }
    }

    private void validateResponse(GoogleBooksApiResponseDto responseDto) {
        if (responseDto == null || responseDto.getItems() == null || responseDto.getItems().isEmpty()) {
            throw new ResourceNotFoundException("No books found for the given search criteria");
        }
    }

    @Async
    public void logGoogleBooksSearch(String searchType, String value) {
        log.info("Asynchronous Google Books search [{}]: {}", searchType, value);
    }

    private GoogleBooksApiResponseDto filterAllowedLanguages(
            GoogleBooksApiResponseDto responseDto
    ) {
        validateResponse(responseDto);

        List<GoogleBookVolumeDto> filteredItems =
                responseDto.getItems().stream()
                        .filter(book ->
                                book != null
                                        && book.getVolumeInfo() != null
                                        && book.getVolumeInfo().getLanguage() != null
                                        && ALLOWED_LANGUAGES.contains(
                                        book.getVolumeInfo()
                                                .getLanguage()
                                                .trim()
                                                .toLowerCase()
                                )
                        )
                        .toList();

        if (filteredItems.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No books found in Spanish or English for the given search criteria"
            );
        }

        responseDto.setItems(filteredItems);

        return responseDto;
    }
}


