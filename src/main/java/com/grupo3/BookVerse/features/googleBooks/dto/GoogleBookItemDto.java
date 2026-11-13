package com.grupo3.BookVerse.features.googleBooks.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GoogleBookItemDto {
    private String title;
    private List<String> authors;
    private String description;
    private List<String> categories;
    private String publisher;
    private String publishedDate;
    private Integer pageCount;
    private String language;
    private GoogleBookImageLinksDto imageLinks;
    private List<GoogleBookIndustryIdentifierDto> industryIdentifiers;
}