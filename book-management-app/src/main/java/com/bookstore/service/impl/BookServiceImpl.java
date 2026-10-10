package com.bookstore.service.impl;

import com.bookstore.dto.BookRequest;
import com.bookstore.dto.BookResponse;
import com.bookstore.exception.ResourceNotFoundException;
import com.bookstore.exception.DuplicateResourceException;
import com.bookstore.model.Book;
import com.bookstore.repository.BookRepository;
import com.bookstore.service.BookService;
import com.bookstore.util.TelemetryUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookServiceImpl implements BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookServiceImpl.class);
    private final BookRepository bookRepository;

    @Autowired
    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public BookResponse createBook(BookRequest bookRequest) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("operation", "createBook");
        attributes.put("book.isbn", bookRequest.getIsbn());
        attributes.put("book.title", bookRequest.getTitle());
        attributes.put("book.author", bookRequest.getAuthor());
        attributes.put("book.category", bookRequest.getCategory() != null ? bookRequest.getCategory() : "N/A");
        attributes.put("book.price", bookRequest.getPrice().doubleValue());

        return TelemetryUtils.builder("BookService.createBook")
                .withAttributes(attributes)
                .withLogger(logger)
                .execute(() -> {
                    logger.info("Creating new book with ISBN: {}", bookRequest.getIsbn());

                    // Check if book with same ISBN already exists
                    if (bookRepository.findByIsbn(bookRequest.getIsbn()).isPresent()) {
                        TelemetryUtils.addSpanAttribute("error.type", "DuplicateResource");
                        logger.error("Book with ISBN {} already exists", bookRequest.getIsbn());
                        throw new DuplicateResourceException("Book with ISBN " + bookRequest.getIsbn() + " already exists");
                    }

                    Book book = new Book();
                    book.setTitle(bookRequest.getTitle());
                    book.setAuthor(bookRequest.getAuthor());
                    book.setIsbn(bookRequest.getIsbn());
                    book.setDescription(bookRequest.getDescription());
                    book.setPrice(bookRequest.getPrice());
                    book.setPublicationYear(bookRequest.getPublicationYear());
                    book.setAvailableQuantity(bookRequest.getAvailableQuantity());
                    book.setPublisher(bookRequest.getPublisher());
                    book.setCategory(bookRequest.getCategory());

                    Book savedBook = bookRepository.save(book);

                    // Add result attributes
                    TelemetryUtils.addSpanAttribute("book.id", savedBook.getId());
                    TelemetryUtils.addSpanEvent("Book created successfully");

                    logger.info("Successfully created book with ID: {}", savedBook.getId());

                    return new BookResponse(savedBook);
                });
    }

    @Override
    public BookResponse getBookById(Long id) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("operation", "getBookById");
        attributes.put("book.id", id);

        return TelemetryUtils.builder("BookService.getBookById")
                .withAttributes(attributes)
                .withLogger(logger)
                .execute(() -> {
                    logger.info("Fetching book with ID: {}", id);

                    Book book = bookRepository.findById(id)
                            .orElseThrow(() -> {
                                TelemetryUtils.addSpanAttribute("error.type", "ResourceNotFound");
                                logger.error("Book not found with ID: {}", id);
                                return new ResourceNotFoundException("Book not found with id: " + id);
                            });

                    // Add retrieved book details to span
                    Map<String, Object> bookAttributes = new HashMap<>();
                    bookAttributes.put("book.isbn", book.getIsbn());
                    bookAttributes.put("book.title", book.getTitle());
                    bookAttributes.put("book.author", book.getAuthor());
                    TelemetryUtils.addSpanAttributes(bookAttributes);
                    TelemetryUtils.addSpanEvent("Book retrieved successfully");

                    logger.info("Successfully retrieved book: {} by {}", book.getTitle(), book.getAuthor());

                    return new BookResponse(book);
                });
    }

    @Override
    public List<BookResponse> getAllBooks() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("operation", "getAllBooks");

        return TelemetryUtils.builder("BookService.getAllBooks")
                .withAttributes(attributes)
                .withLogger(logger)
                .execute(() -> {
                    logger.info("Fetching all books");

                    List<BookResponse> books = bookRepository.findAll().stream()
                            .map(BookResponse::new)
                            .collect(Collectors.toList());

                    // Add result metrics to span
                    TelemetryUtils.addSpanAttribute("books.count", books.size());
                    Map<String, Object> eventAttrs = new HashMap<>();
                    eventAttrs.put("count", (long) books.size());
                    TelemetryUtils.addSpanEvent("Books retrieved", eventAttrs);

                    logger.info("Successfully retrieved {} books", books.size());

                    return books;
                });
    }

    @Override
    public BookResponse updateBook(Long id, BookRequest bookRequest) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("operation", "updateBook");
        attributes.put("book.id", id);
        attributes.put("book.isbn", bookRequest.getIsbn());

        return TelemetryUtils.builder("BookService.updateBook")
                .withAttributes(attributes)
                .withLogger(logger)
                .execute(() -> {
                    logger.info("Updating book with ID: {}", id);

                    Book book = bookRepository.findById(id)
                            .orElseThrow(() -> {
                                TelemetryUtils.addSpanAttribute("error.type", "ResourceNotFound");
                                logger.error("Book not found with ID: {}", id);
                                return new ResourceNotFoundException("Book not found with id: " + id);
                            });

                    // Track if ISBN is being changed
                    boolean isbnChanged = !book.getIsbn().equals(bookRequest.getIsbn());
                    TelemetryUtils.addSpanAttribute("book.isbn_changed", isbnChanged);

                    if (isbnChanged) {
                        Map<String, Object> eventAttrs = new HashMap<>();
                        eventAttrs.put("old_isbn", book.getIsbn());
                        eventAttrs.put("new_isbn", bookRequest.getIsbn());
                        TelemetryUtils.addSpanEvent("ISBN change detected", eventAttrs);

                        if (bookRepository.findByIsbn(bookRequest.getIsbn()).isPresent()) {
                            TelemetryUtils.addSpanAttribute("error.type", "DuplicateResource");
                            logger.error("Cannot update - ISBN {} already exists", bookRequest.getIsbn());
                            throw new DuplicateResourceException("Book with ISBN " + bookRequest.getIsbn() + " already exists");
                        }
                    }

                    book.setTitle(bookRequest.getTitle());
                    book.setAuthor(bookRequest.getAuthor());
                    book.setIsbn(bookRequest.getIsbn());
                    book.setDescription(bookRequest.getDescription());
                    book.setPrice(bookRequest.getPrice());
                    book.setPublicationYear(bookRequest.getPublicationYear());
                    book.setAvailableQuantity(bookRequest.getAvailableQuantity());
                    book.setPublisher(bookRequest.getPublisher());
                    book.setCategory(bookRequest.getCategory());

                    Book updatedBook = bookRepository.save(book);

                    TelemetryUtils.addSpanEvent("Book updated successfully");
                    logger.info("Successfully updated book ID: {} - {}", updatedBook.getId(), updatedBook.getTitle());

                    return new BookResponse(updatedBook);
                });
    }

    @Override
    public void deleteBook(Long id) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("operation", "deleteBook");
        attributes.put("book.id", id);

        TelemetryUtils.builder("BookService.deleteBook")
                .withAttributes(attributes)
                .withLogger(logger)
                .executeVoid(() -> {
                    logger.info("Attempting to delete book with ID: {}", id);

                    if (!bookRepository.existsById(id)) {
                        TelemetryUtils.addSpanAttribute("error.type", "ResourceNotFound");
                        logger.error("Cannot delete - book not found with ID: {}", id);
                        throw new ResourceNotFoundException("Book not found with id: " + id);
                    }

                    bookRepository.deleteById(id);

                    TelemetryUtils.addSpanEvent("Book deleted successfully");
                    logger.info("Successfully deleted book with ID: {}", id);
                });
    }

    @Override
    public List<BookResponse> searchBooksByTitle(String title) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("operation", "searchBooksByTitle");
        attributes.put("search.title", title);
        attributes.put("search.type", "title");

        return TelemetryUtils.builder("BookService.searchBooksByTitle")
                .withAttributes(attributes)
                .withLogger(logger)
                .execute(() -> {
                    logger.info("Searching books by title: {}", title);

                    List<BookResponse> books = bookRepository.findByTitleContainingIgnoreCase(title).stream()
                            .map(BookResponse::new)
                            .collect(Collectors.toList());

                    TelemetryUtils.addSpanAttribute("search.results_count", books.size());
                    Map<String, Object> eventAttrs = new HashMap<>();
                    eventAttrs.put("results", (long) books.size());
                    TelemetryUtils.addSpanEvent("Search completed", eventAttrs);

                    logger.info("Search completed: found {} books matching title '{}'", books.size(), title);

                    return books;
                });
    }

    @Override
    public List<BookResponse> getBooksByAuthor(String author) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("operation", "getBooksByAuthor");
        attributes.put("search.author", author);
        attributes.put("search.type", "author");

        return TelemetryUtils.builder("BookService.getBooksByAuthor")
                .withAttributes(attributes)
                .withLogger(logger)
                .execute(() -> {
                    logger.info("Fetching books by author: {}", author);

                    List<BookResponse> books = bookRepository.findByAuthor(author).stream()
                            .map(BookResponse::new)
                            .collect(Collectors.toList());

                    TelemetryUtils.addSpanAttribute("search.results_count", books.size());
                    logger.info("Found {} books by author '{}'", books.size(), author);

                    return books;
                });
    }

    @Override
    public List<BookResponse> getBooksByCategory(String category) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("operation", "getBooksByCategory");
        attributes.put("search.category", category);
        attributes.put("search.type", "category");

        return TelemetryUtils.builder("BookService.getBooksByCategory")
                .withAttributes(attributes)
                .withLogger(logger)
                .execute(() -> {
                    logger.info("Fetching books by category: {}", category);

                    List<BookResponse> books = bookRepository.findByCategory(category).stream()
                            .map(BookResponse::new)
                            .collect(Collectors.toList());

                    TelemetryUtils.addSpanAttribute("search.results_count", books.size());
                    logger.info("Found {} books in category '{}'", books.size(), category);

                    return books;
                });
    }

    @Override
    public BookResponse getBookByIsbn(String isbn) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("operation", "getBookByIsbn");
        attributes.put("book.isbn", isbn);
        attributes.put("search.type", "isbn");

        return TelemetryUtils.builder("BookService.getBookByIsbn")
                .withAttributes(attributes)
                .withLogger(logger)
                .execute(() -> {
                    logger.info("Fetching book by ISBN: {}", isbn);

                    Book book = bookRepository.findByIsbn(isbn)
                            .orElseThrow(() -> {
                                TelemetryUtils.addSpanAttribute("error.type", "ResourceNotFound");
                                logger.error("Book not found with ISBN: {}", isbn);
                                return new ResourceNotFoundException("Book not found with ISBN: " + isbn);
                            });

                    Map<String, Object> bookAttributes = new HashMap<>();
                    bookAttributes.put("book.id", book.getId());
                    bookAttributes.put("book.title", book.getTitle());
                    bookAttributes.put("book.author", book.getAuthor());
                    TelemetryUtils.addSpanAttributes(bookAttributes);

                    logger.info("Successfully retrieved book: {} by ISBN: {}", book.getTitle(), isbn);

                    return new BookResponse(book);
                });
    }
}

