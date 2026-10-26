package com.grupo3.BookVerse.features.googleBooks.client;

import com.grupo3.BookVerse.config.properties.GoogleBooksProperties;
import com.grupo3.BookVerse.features.googleBooks.dto.GoogleBookVolumeDto;
import com.grupo3.BookVerse.features.googleBooks.dto.GoogleBooksApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class GoogleBooksClient {

    private static final int MAX_RESULTS = 20;

    private final GoogleBooksProperties googleBooksProperties;
    private final RestTemplate restTemplate;

    public GoogleBooksApiResponseDto searchBooks(String query) {

        String url = UriComponentsBuilder
                .fromUriString(googleBooksProperties.getUrl())
                .queryParam("q", query)
                .queryParam("printType", "books")
                .queryParam("maxResults", MAX_RESULTS)
                .queryParam("orderBy", "relevance")
                .queryParam("key", googleBooksProperties.getKey())
                .toUriString();

        return restTemplate.getForObject(
                url,
                GoogleBooksApiResponseDto.class
        );
    }

    public GoogleBookVolumeDto getBookByGoogleId(String googleBookId) {

        String url = UriComponentsBuilder
                .fromUriString(googleBooksProperties.getUrl() + "/" + googleBookId
                )
                .queryParam("key",googleBooksProperties.getKey()
                )
                .toUriString();

        return restTemplate.getForObject(url,GoogleBookVolumeDto.class
        );
    }
}