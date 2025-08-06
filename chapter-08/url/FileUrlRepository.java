package com.shareround.demo.url;

import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@Component
@Slf4j
public class FileUrlRepository {

    private final ObjectMapper objectMapper;
    private final String filePath;
    private final Map<String, UrlMapping> urlMappingCache = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public FileUrlRepository(@Value("${url.storage.file.path:url-mappings.json}") String filePath) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.filePath = filePath;
        loadFromFile();
    }

    private void loadFromFile() {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.info("URL 매핑 파일이 존재하지 않음, 새로 생성: {}", filePath);
                saveToFile();
                return;
            }

            TypeReference<Map<String, UrlMapping>> typeRef = new TypeReference<Map<String, UrlMapping>>() {
            };
            Map<String, UrlMapping> loadedMappings = objectMapper.readValue(file, typeRef);

            urlMappingCache.clear();
            urlMappingCache.putAll(loadedMappings);

            // ID 생성기 초기화
            long maxId = urlMappingCache.values().stream()
                    .mapToLong(mapping -> mapping.getId() != null ? mapping.getId() : 0)
                    .max()
                    .orElse(0);
            idGenerator.set(maxId + 1);

            log.info("URL 매핑 파일 로드 완료: {} 개 매핑", urlMappingCache.size());
        } catch (IOException e) {
            log.error("URL 매핑 파일 로드 실패: {}", e.getMessage());
            throw new RuntimeException("URL 매핑 파일 로드 실패", e);
        }
    }

    private synchronized void saveToFile() {
        try {
            objectMapper.writeValue(new File(filePath), urlMappingCache);
            log.debug("URL 매핑 파일 저장 완료");
        } catch (IOException e) {
            log.error("URL 매핑 파일 저장 실패: {}", e.getMessage());
            throw new RuntimeException("URL 매핑 파일 저장 실패", e);
        }
    }

    public Optional<UrlMapping> findByLongUrl(String longUrl) {
        return urlMappingCache.values().stream()
                .filter(mapping -> mapping.getLongUrl().equals(longUrl))
                .findFirst();
    }

    public Optional<UrlMapping> findByShortUrl(String shortUrl) {
        return Optional.ofNullable(urlMappingCache.get(shortUrl));
    }

    public boolean existsByShortUrl(String shortUrl) {
        return urlMappingCache.containsKey(shortUrl);
    }

    public UrlMapping save(UrlMapping urlMapping) {
        if (urlMapping.getId() == null) {
            urlMapping.setId(idGenerator.getAndIncrement());
        }

        urlMappingCache.put(urlMapping.getShortUrl(), urlMapping);
        saveToFile();

        log.debug("URL 매핑 저장: {} -> {}", urlMapping.getLongUrl(), urlMapping.getShortUrl());
        return urlMapping;
    }

    public void incrementAccessCount(String shortUrl) {
        UrlMapping mapping = urlMappingCache.get(shortUrl);
        if (mapping != null) {
            mapping.setAccessCount(mapping.getAccessCount() + 1);
            saveToFile();
        }
    }

    public long count() {
        return urlMappingCache.size();
    }


    public List<UrlMapping> findAll() {
        return new ArrayList<>(urlMappingCache.values());
    }


    public void refresh() {
        loadFromFile();
    }
}
