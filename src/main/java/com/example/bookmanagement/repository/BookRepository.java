package com.example.bookmanagement.repository;

import com.example.bookmanagement.domain.Book;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class BookRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<Book> bookRowMapper = (rs, rowNum) -> new Book(
            rs.getLong("id"),
            rs.getString("title"),
            rs.getString("author"),
            rs.getString("isbn"),
            rs.getBigDecimal("price"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    public BookRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Book save(Book book) {
        String sql = "INSERT INTO books (title, author, isbn, price) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, book.getTitle(), book.getAuthor(), book.getIsbn(), book.getPrice());
        return findByIsbn(book.getIsbn()).orElse(null);
    }

    public List<Book> findAll() {
        String sql = "SELECT * FROM books ORDER BY id DESC";
        return jdbcTemplate.query(sql, bookRowMapper);
    }

    public Optional<Book> findById(Long id) {
        String sql = "SELECT * FROM books WHERE id = ?";
        return jdbcTemplate.query(sql, bookRowMapper, id)
                .stream()
                .findFirst();
    }

    public Optional<Book> findByIsbn(String isbn) {
        String sql = "SELECT * FROM books WHERE isbn = ?";
        return jdbcTemplate.query(sql, bookRowMapper, isbn)
                .stream()
                .findFirst();
    }

    public int update(Long id, Book book) {
        String sql = "UPDATE books SET title = ?, author = ?, isbn = ?, price = ? WHERE id = ?";
        return jdbcTemplate.update(sql, book.getTitle(), book.getAuthor(), book.getIsbn(), book.getPrice(), id);
    }

    public int deleteById(Long id) {
        String sql = "DELETE FROM books WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
