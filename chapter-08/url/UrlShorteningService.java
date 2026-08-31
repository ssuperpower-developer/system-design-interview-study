package com.shareround.demo.url;

import lombok.Getter;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.CRC32;

@Getter
@Service
@Slf4j
public class UrlShorteningService {

    private final FileUrlRepository urlRepository;

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int SHORT_URL_LENGTH = 8;
    private static final int MAX_COLLISION_ATTEMPTS = 10;

    public UrlShorteningService(FileUrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    public UrlShortenResponse shortenUrl(String longUrl) {
        log.info("URL 단축 요청: {}", longUrl);

        Optional<UrlMapping> existingMapping = urlRepository.findByLongUrl(longUrl);
        if (existingMapping.isPresent()) {
            UrlMapping mapping = existingMapping.get();
            log.info("기존 단축 URL 반환: {}", mapping.getShortUrl());
            return new UrlShortenResponse(mapping.getShortUrl(), mapping.getLongUrl(), mapping.getCreatedAt());
        }

        String shortUrl = generateShortUrl(longUrl);

        UrlMapping mapping = new UrlMapping(longUrl, shortUrl);
        mapping = urlRepository.save(mapping);

        log.info("새로운 단축 URL 생성: {} -> {}", longUrl, shortUrl);
        return new UrlShortenResponse(mapping.getShortUrl(), mapping.getLongUrl(), mapping.getCreatedAt());
    }

    public String redirectUrl(String shortUrl) {
        log.info("URL 리디렉션 요청: {}", shortUrl);

        UrlMapping mapping = urlRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new RuntimeException("단축 URL을 찾을 수 없습니다: " + shortUrl));

        urlRepository.incrementAccessCount(shortUrl);

        log.info("파일에서 URL 반환: {}", mapping.getLongUrl());
        return mapping.getLongUrl();
    }

    private String generateShortUrl(String longUrl) {
        String originalUrl = longUrl;
        int attempts = 0;

        while (attempts < MAX_COLLISION_ATTEMPTS) {
            // CRC32 해시 계산
            CRC32 crc32 = new CRC32();
            crc32.update(originalUrl.getBytes(StandardCharsets.UTF_8));
            long hashValue = crc32.getValue();

            String shortUrl = encodeToBase62(hashValue);

            if (!urlRepository.existsByShortUrl(shortUrl)) {
                return shortUrl;
            }

            originalUrl = longUrl + "_" + attempts;
            attempts++;
            log.warn("단축 URL 충돌 발생, 재시도 {}/{}: {}", attempts, MAX_COLLISION_ATTEMPTS, shortUrl);
        }

        throw new RuntimeException("단축 URL 생성에 실패했습니다. 최대 시도 횟수 초과");
    }

    private String encodeToBase62(long value) {
        if (value == 0) {
            return "0".repeat(UrlShorteningService.SHORT_URL_LENGTH);
        }

        StringBuilder result = new StringBuilder();
        long num = Math.abs(value); // 음수 방지

        while (num > 0) {
            result.insert(0, BASE62_CHARS.charAt((int) (num % 62)));
            num /= 62;
        }

        while (result.length() < UrlShorteningService.SHORT_URL_LENGTH) {
            result.insert(0, '0');
        }

        if (result.length() > UrlShorteningService.SHORT_URL_LENGTH) {
            result = new StringBuilder(result.substring(result.length() - UrlShorteningService.SHORT_URL_LENGTH));
        }

        return result.toString();
    }

    public long getTotalMappings() {
        return urlRepository.count();
    }
}
