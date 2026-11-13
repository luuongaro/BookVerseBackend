package com.grupo3.BookVerse.features.googleBooks.service.impl;

import com.grupo3.BookVerse.common.exception.BadRequestException;
import com.grupo3.BookVerse.common.exception.ResourceNotFoundException;
import com.grupo3.BookVerse.features.googleBooks.client.GoogleBooksClient;
import com.grupo3.BookVerse.features.googleBooks.dto.GoogleBookItemDto;
import com.grupo3.BookVerse.features.googleBooks.dto.GoogleBookVolumeDto;
import com.grupo3.BookVerse.features.googleBooks.dto.GoogleBooksApiResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleBooksServiceImplTest {

    @Mock
    private GoogleBooksClient googleBooksClient;

    @InjectMocks
    private GoogleBooksServiceImpl googleBooksService;

    // Verifies that the service correctly searches books using a valid query
    @Test
    void shouldSearchBooksSuccessfully() {

        GoogleBookItemDto item =
                new GoogleBookItemDto();

        item.setTitle("Harry Potter");
        item.setLanguage("en");

        GoogleBookVolumeDto volume =
                new GoogleBookVolumeDto();

        volume.setVolumeInfo(item);

        GoogleBooksApiResponseDto response =
                new GoogleBooksApiResponseDto();

        response.setItems(List.of(volume));

        when(googleBooksClient.searchBooks("Harry Potter"))
                .thenReturn(response);

        GoogleBooksApiResponseDto result =
                googleBooksService.searchBooks("Harry Potter");

        assertNotNull(result);

        assertEquals(
                1,
                result.getItems().size()
        );

        verify(googleBooksClient)
                .searchBooks("Harry Potter");
    }


    //Verifies that the service throws an exception when the search query is null or empty
    @Test
    void shouldThrowExceptionWhenQueryIsEmpty() {

        assertThrows(
                BadRequestException.class,
                () -> googleBooksService.searchBooks(" ")
        );

        verify(googleBooksClient, never())
                .searchBooks(anyString());
    }

    //Verifies that the service correctly searches books by title
    @Test
    void shouldSearchBooksByTitleSuccessfully() {

        GoogleBooksApiResponseDto response =
                new GoogleBooksApiResponseDto();

        GoogleBookItemDto info = new GoogleBookItemDto();
        info.setTitle("Book");
        info.setLanguage("en");
        GoogleBookVolumeDto volume = new GoogleBookVolumeDto();
        volume.setVolumeInfo(info);
        response.setItems(List.of(volume));

        when(googleBooksClient
                .searchBooks("intitle:Harry Potter"))
                .thenReturn(response);

        GoogleBooksApiResponseDto result =
                googleBooksService
                        .searchBooksByTitle("Harry Potter");

        assertNotNull(result);

        verify(googleBooksClient)
                .searchBooks("intitle:Harry Potter");
    }

    //Verifies that the service correctly searches books by author
    @Test
    void shouldSearchBooksByAuthorSuccessfully() {

        GoogleBooksApiResponseDto response =
                new GoogleBooksApiResponseDto();

        GoogleBookItemDto info = new GoogleBookItemDto();
        info.setTitle("Book");
        info.setLanguage("en");
        GoogleBookVolumeDto volume = new GoogleBookVolumeDto();
        volume.setVolumeInfo(info);
        response.setItems(List.of(volume));

        when(googleBooksClient
                .searchBooks("inauthor:J K Rowling"))
                .thenReturn(response);

        GoogleBooksApiResponseDto result =
                googleBooksService
                        .searchBooksByAuthor("J K Rowling");

        assertNotNull(result);

        verify(googleBooksClient)
                .searchBooks("inauthor:J K Rowling");
    }

    //Verifies that the service correctly retrieves a book by Google Book ID
    @Test
    void shouldGetBookByGoogleIdSuccessfully() {

        String googleBookId = "book123";

        GoogleBookItemDto item =
                new GoogleBookItemDto();

        item.setTitle("Spring Boot");
        item.setLanguage("en");

        GoogleBookVolumeDto book =
                new GoogleBookVolumeDto();

        book.setId(googleBookId);
        book.setVolumeInfo(item);

        when(googleBooksClient
                .getBookByGoogleId(googleBookId))
                .thenReturn(book);

        GoogleBookVolumeDto result =
                googleBooksService
                        .getBookByGoogleId(googleBookId);

        assertNotNull(result);

        assertEquals(
                googleBookId,
                result.getId()
        );

        verify(googleBooksClient)
                .getBookByGoogleId(googleBookId);


    }

    //Verifies that the service throws an exception when the book does not exist in Google Books
    @Test
    void shouldThrowExceptionWhenBookNotFound() {

        String googleBookId = "invalidId";

        when(googleBooksClient
                .getBookByGoogleId(googleBookId))
                .thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> googleBooksService
                        .getBookByGoogleId(googleBookId)
        );

        verify(googleBooksClient)
                .getBookByGoogleId(googleBookId);
    }

    //search books returns null
    @Test
    void shouldThrowExceptionWhenSearchResponseIsNull() {
        when(googleBooksClient.searchBooks("Harry Potter"))
                .thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> googleBooksService.searchBooks("Harry Potter")
        );

        verify(googleBooksClient).searchBooks("Harry Potter");
    }

    //search books returns empty list
    @Test
    void shouldThrowExceptionWhenSearchReturnsEmptyItems() {
        GoogleBooksApiResponseDto response = new GoogleBooksApiResponseDto();
        response.setItems(List.of());

        when(googleBooksClient.searchBooks("Harry Potter"))
                .thenReturn(response);

        assertThrows(
                ResourceNotFoundException.class,
                () -> googleBooksService.searchBooks("Harry Potter")
        );

        verify(googleBooksClient).searchBooks("Harry Potter");
    }
    //search books reaturns items = null
    @Test
    void shouldThrowExceptionWhenSearchReturnsNullItems() {
        GoogleBooksApiResponseDto response = new GoogleBooksApiResponseDto();
        response.setItems(null);

        when(googleBooksClient.searchBooks("Harry Potter"))
                .thenReturn(response);

        assertThrows(
                ResourceNotFoundException.class,
                () -> googleBooksService.searchBooks("Harry Potter")
        );

        verify(googleBooksClient).searchBooks("Harry Potter");
    }
    //Query null
    @Test
    void shouldThrowExceptionWhenQueryIsNull() {
        assertThrows(
                BadRequestException.class,
                () -> googleBooksService.searchBooks(null)
        );

        verify(googleBooksClient, never()).searchBooks(anyString());
    }
    //empty title
    @Test
    void shouldThrowExceptionWhenTitleIsEmpty() {
        assertThrows(
                BadRequestException.class,
                () -> googleBooksService.searchBooksByTitle(" ")
        );

        verify(googleBooksClient, never()).searchBooks(anyString());
    }
    //empty author
    @Test
    void shouldThrowExceptionWhenAuthorIsEmpty() {
        assertThrows(
                BadRequestException.class,
                () -> googleBooksService.searchBooksByAuthor(" ")
        );

        verify(googleBooksClient, never()).searchBooks(anyString());
    }
    //empty google book id
    @Test
    void shouldThrowExceptionWhenGoogleBookIdIsEmpty() {
        assertThrows(
                BadRequestException.class,
                () -> googleBooksService.getBookByGoogleId(" ")
        );

        verify(googleBooksClient, never()).getBookByGoogleId(anyString());
    }

    //book found but without volumeInfo
    @Test
    void shouldThrowExceptionWhenBookVolumeInfoIsNull() {
        String googleBookId = "book123";

        GoogleBookVolumeDto book = new GoogleBookVolumeDto();
        book.setId(googleBookId);
        book.setVolumeInfo(null);

        when(googleBooksClient.getBookByGoogleId(googleBookId))
                .thenReturn(book);

        assertThrows(
                ResourceNotFoundException.class,
                () -> googleBooksService.getBookByGoogleId(googleBookId)
        );

        verify(googleBooksClient).getBookByGoogleId(googleBookId);
    }
}