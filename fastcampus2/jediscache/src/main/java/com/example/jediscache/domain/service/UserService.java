package com.example.jediscache.domain.service;

import com.example.jediscache.domain.entity.RedisHashUser;
import com.example.jediscache.domain.entity.User;
import com.example.jediscache.domain.repository.RedisHashUserRepository;
import com.example.jediscache.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.example.jediscache.config.CacheConfig.CACHE1;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RedisHashUserRepository redisHashUserRepository;
    private final RedisTemplate<String, User> userRedisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    public User getUser(Long id) {
        var key = "users:%d".formatted(id);
        var cachedUser = userRedisTemplate.opsForValue().get(key);

        if(cachedUser != null) {
            return cachedUser;
        }
        User user = userRepository.findById(id).orElseThrow();
        userRedisTemplate.opsForValue().set(key, user, Duration.ofSeconds(30));

        return userRepository.findById(id).orElseThrow();
    }

    public RedisHashUser getUser2(Long id) {
        var cachedUser = redisHashUserRepository.findById(id).orElseGet(() -> {
            User user = userRepository.findById(id).orElseThrow();
            return redisHashUserRepository.save(RedisHashUser.builder()
                    .id(user.getId())
                    .name(user.getName())
                    .email(user.getEmail())
                    .createAt(user.getCreateAt())
                    .updateAt(user.getUpdateAt())
                    .build());
        });

        return cachedUser;
    }

    @Cacheable(cacheNames = CACHE1, key = "'user:' + #id")
    public User getUser3(Long id) {
        return userRepository.findById(id).orElseThrow();
    }
}
