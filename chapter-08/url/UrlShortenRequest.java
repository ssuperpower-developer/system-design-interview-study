package com.shareround.demo.url;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlShortenRequest {

    private String longUrl;
}
