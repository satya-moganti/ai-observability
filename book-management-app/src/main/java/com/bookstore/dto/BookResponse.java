package com.bookstore.dto;

import com.bookstore.model.Book;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String description;
    private BigDecimal price;
    private Integer publicationYear;
    private Integer availableQuantity;
    private String publisher;
    private String category;

    public BookResponse(Book book) {
        this.id = book.getId();
        this.title = book.getTitle();
        this.author = book.getAuthor();
        this.isbn = book.getIsbn();
        this.description = book.getDescription();
        this.price = book.getPrice();
        this.publicationYear = book.getPublicationYear();
        this.availableQuantity = book.getAvailableQuantity();
        this.publisher = book.getPublisher();
        this.category = book.getCategory();
    }
}
