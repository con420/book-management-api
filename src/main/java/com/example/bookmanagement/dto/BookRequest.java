package com.example.bookmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {
    private String title;
    private String author;
    private String isbn;
    private BigDecimal price;
}
