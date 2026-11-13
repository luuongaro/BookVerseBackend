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
    private static final String SEARCH_FIELDS =
            "items(id,volumeInfo(title,authors,description,categories,publisher," +
                    "publishedDate,pageCount,language,imageLinks/thumbnail," +
                    "industryIdentifiers))";
    private final GoogleBooksProperties googleBooksProperties;
    private final RestTemplate restTemplate;

    public GoogleBooksApiResponseDto searchBooks(String query) {

        URI uri = UriComponentsBuilder
                .fromUriString(googleBooksProperties.getUrl())
                .queryParam("q", query)
                .queryParam("printType", "books")
                .queryParam("maxResults", MAX_RESULTS)
                .queryParam("orderBy", "relevance")
                .queryParam("fields", SEARCH_FIELDS)
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