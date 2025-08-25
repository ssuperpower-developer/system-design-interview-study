package com.shareround.demo.url;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlShortenResponse {
    private String shortUrl;
    private String longUrl;
    private LocalDateTime createdAt;
}
