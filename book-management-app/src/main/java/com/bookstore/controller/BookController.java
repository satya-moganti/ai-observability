package com.bookstore.controller;

import com.bookstore.dto.BookRequest;
import com.bookstore.dto.BookResponse;
import com.bookstore.service.BookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import java.util.List;
@RestController
@RequestMapping("/api/books")
public class BookController {
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    private final BookService bookService;

    @Autowired
    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest bookRequest) {
       logger.info("BookController createBook Request  {} ",bookRequest.getDescription());
        BookResponse bookResponse = bookService.createBook(bookRequest);
        long bookId = (null != bookResponse) ? bookResponse.getId() : 0;
        logger.info("BookController createBook Response  {} ",bookId);

        return new ResponseEntity<>(bookResponse, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable Long id) {
        logger.info("BookController getBookById Request  {} ",id);
        BookResponse bookResponse = bookService.getBookById(id);
        logger.info("BookController getBookById Response  {} ",bookResponse);

        return ResponseEntity.ok(bookResponse);
    }

    @GetMapping
    public ResponseEntity<List<BookResponse>> getAllBooks() {
        logger.info("BookController getAllBooks Request");
        List<BookResponse> books = bookService.getAllBooks();
        logger.info("BookController getAllBooks Response {} ",books.size());
        return ResponseEntity.ok(books);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookResponse> updateBook(
            @PathVariable Long id,
            @Valid @RequestBody BookRequest bookRequest) {
        logger.info("BookController updateBook Request {} {}",id,bookRequest);

        BookResponse bookResponse = bookService.updateBook(id, bookRequest);
        return ResponseEntity.ok(bookResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<BookResponse>> searchBooksByTitle(@RequestParam String title) {
        logger.info("BookController searchBooksByTitle Request {}",title);
        List<BookResponse> books = bookService.searchBooksByTitle(title);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/author/{author}")
    public ResponseEntity<List<BookResponse>> getBooksByAuthor(@PathVariable String author) {
        logger.info("BookController getBooksByAuthor Request {}",author);
        List<BookResponse> books = bookService.getBooksByAuthor(author);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<BookResponse>> getBooksByCategory(@PathVariable String category) {
        logger.info("BookController getBooksByCategory Request {}",category);
        List<BookResponse> books = bookService.getBooksByCategory(category);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookResponse> getBookByIsbn(@PathVariable String isbn) {
        logger.info("BookController getBookByIsbn Request {}",isbn);
        BookResponse bookResponse = bookService.getBookByIsbn(isbn);
        return ResponseEntity.ok(bookResponse);
    }
}
