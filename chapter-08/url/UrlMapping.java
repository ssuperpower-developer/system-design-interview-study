package com.shareround.demo.url;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlMapping {

    private Long id;
    private String longUrl;
    private String shortUrl;
    private LocalDateTime createdAt;
    private Long accessCount;

    public UrlMapping(String longUrl, String shortUrl) {
        this.longUrl = longUrl;
        this.shortUrl = shortUrl;
        this.createdAt = LocalDateTime.now();
        this.accessCount = 0L;
    }
}
