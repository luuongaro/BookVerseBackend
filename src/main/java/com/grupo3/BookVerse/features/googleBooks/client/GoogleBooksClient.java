package com.grupo3.BookVerse.features.googleBooks.client;

import com.grupo3.BookVerse.config.properties.GoogleBooksProperties;
import com.grupo3.BookVerse.features.googleBooks.dto.GoogleBookVolumeDto;
import com.grupo3.BookVerse.features.googleBooks.dto.GoogleBooksApiResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class GoogleBooksClient {

    private static final int MAX_RESULTS = 20;

    private final GoogleBooksProperties googleBooksProperties;
    private final RestTemplate restTemplate;

    public GoogleBooksApiResponseDto searchBooks(String query) {

        URI uri = UriComponentsBuilder
                .fromUriString(googleBooksProperties.getUrl())
                .queryParam("q", query)
                .queryParam("printType", "books")
                .queryParam("maxResults", 20)
                .queryParam("orderBy", "relevance")
                .queryParam("key", googleBooksProperties.getKey())
                .build()
                .encode()
                .toUri();

        return restTemplate.getForObject(
                uri,
                GoogleBooksApiResponseDto.class
        );
    }

    public GoogleBookVolumeDto getBookByGoogleId(
            String googleBookId
    ) {

        URI uri = UriComponentsBuilder
                .fromUriString(
                        googleBooksProperties.getUrl()
                                + "/"
                                + googleBookId
                )
                .queryParam(
                        "key",
                        googleBooksProperties.getKey()
                )
                .build()
                .encode()
                .toUri();

        return restTemplate.getForObject(
                uri,
                GoogleBookVolumeDto.class
        );
    }
}