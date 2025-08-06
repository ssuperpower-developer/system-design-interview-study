package com.example.jediscache.controller;

import com.example.jediscache.domain.entity.RedisHashUser;
import com.example.jediscache.domain.entity.User;
import com.example.jediscache.domain.repository.UserRepository;
import com.example.jediscache.domain.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserRepository userRepository;
    private final UserService userService;
    private final JedisPool jedisPool;

    @GetMapping("/users/{id}/email")
    public String getEmail(@PathVariable Long id) {
        try(Jedis jedis = jedisPool.getResource()) {
            var userEmailRedisKey = "users:%d:email".formatted(id);

            String userEmail = jedis.get(userEmailRedisKey);
            if(userEmail != null) {
                return userEmail;
            }

            userEmail = userRepository.findById(id).orElse(User.builder().build()).getEmail();
            jedis.set(userEmailRedisKey, userEmail);
            jedis.setex(userEmailRedisKey, 30, userEmail);
            return userEmail;
        }
    }

    @GetMapping("/users/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getUser3(id);
    }

    @GetMapping("/redishash-users/{id}")
    public RedisHashUser getUser2(@PathVariable Long id) {
        return userService.getUser2(id);
    }

    @GetMapping("/")
    public Map<String, String> home(HttpSession session) {
        Integer visitCount = (Integer) session.getAttribute("visits");
        if(visitCount == null) {
            visitCount = 0;
        }
        session.setAttribute("visits", ++visitCount);
        return Map.of("session Id", session.getId(), "visits", visitCount.toString());
    }
}
