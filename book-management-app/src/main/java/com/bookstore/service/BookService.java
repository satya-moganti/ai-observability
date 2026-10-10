package com.bookstore.service;

import com.bookstore.dto.BookRequest;
import com.bookstore.dto.BookResponse;

import java.util.List;

public interface BookService {

    BookResponse createBook(BookRequest bookRequest);

    BookResponse getBookById(Long id);

    List<BookResponse> getAllBooks();

    BookResponse updateBook(Long id, BookRequest bookRequest);

    void deleteBook(Long id);

    List<BookResponse> searchBooksByTitle(String title);

    List<BookResponse> getBooksByAuthor(String author);

    List<BookResponse> getBooksByCategory(String category);

    BookResponse getBookByIsbn(String isbn);
}
