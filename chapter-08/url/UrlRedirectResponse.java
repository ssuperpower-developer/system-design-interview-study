package com.shareround.demo.url;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlRedirectResponse {
    private String longUrl;
    private String shortUrl;
    private Long accessCount;
}
