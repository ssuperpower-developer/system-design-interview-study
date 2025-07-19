package com.shareround.demo.url;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class UrlController {

    private final UrlShorteningService urlShorteningService;

    public UrlController(UrlShorteningService urlShorteningService) {
        this.urlShorteningService = urlShorteningService;
    }

    @PostMapping("/shorten")
    public ResponseEntity<UrlShortenResponse> shortenUrl(@RequestBody UrlShortenRequest request) {
        try {
            UrlShortenResponse response = urlShorteningService.shortenUrl(request.getLongUrl());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("URL 단축 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{shortUrl}")
    public ResponseEntity<Void> redirectUrl(@PathVariable String shortUrl) {
        try {
            String longUrl = urlShorteningService.redirectUrl(shortUrl);

            // 301 Moved Permanently 상태 코드로 리디렉션
            return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY)
                    .location(URI.create(longUrl))
                    .build();
        } catch (RuntimeException e) {
            log.warn("단축 URL을 찾을 수 없음: {}", shortUrl);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("URL 리디렉션 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/info/{shortUrl}")
    public ResponseEntity<UrlRedirectResponse> getUrlInfo(@PathVariable String shortUrl) {
        try {
            // FileUrlRepository를 통해 매핑 정보 직접 조회
            Optional<UrlMapping> mapping = urlShorteningService.getUrlRepository().findByShortUrl(shortUrl);
            if (mapping.isPresent()) {
                UrlMapping urlMapping = mapping.get();
                UrlRedirectResponse response = new UrlRedirectResponse(
                        urlMapping.getLongUrl(),
                        urlMapping.getShortUrl(),
                        urlMapping.getAccessCount()
                );
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("URL 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalMappings", urlShorteningService.getTotalMappings());
            stats.put("timestamp", LocalDateTime.now());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("통계 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
