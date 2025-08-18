package com.examp.Webflux.service;

import com.examp.Webflux.config.EmbeddedRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;
import lombok.extern.slf4j.Slf4j;

@SpringBootTest
@Import(EmbeddedRedisConfig.class)
@ActiveProfiles("test")
@Slf4j
class UserQueueServiceTest {

    @Autowired
    private UserQueueService userQueueService;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @BeforeEach
    public void beforeEach() {
        reactiveRedisTemplate.getConnectionFactory().getReactiveConnection().serverCommands().flushAll().block();
    }

    @Test
    void registerWaitQueue_success() {
        StepVerifier.create(userQueueService.registerWaitQueue("default", 1L))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    void registerWaitQueue_fail_when_already_registered() {
        StepVerifier.create(userQueueService.registerWaitQueue("default", 1L)
                        .then(userQueueService.registerWaitQueue("default", 1L)))
                .expectError()
                .verify();
    }

    @Test
    void allowUser() {
        StepVerifier.create(
                userQueueService.registerWaitQueue("default", 1L)
                        .then(userQueueService.registerWaitQueue("default", 2L))
                        .then(userQueueService.registerWaitQueue("default", 3L))
                        .then(userQueueService.allowUser("default", 2L))
                )
                .expectNext(2L)
                .verifyComplete();
    }

    @Test
    void isAllowed() {
        StepVerifier.create(
                userQueueService.registerWaitQueue("default", 1L)
                        .then(userQueueService.allowUser("default", 1L))
                        .then(userQueueService.isAllowed("default", 1L))
                )
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(
                        userQueueService.registerWaitQueue("default", 2L)
                                .then(userQueueService.isAllowed("default", 2L))
                )
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void generateToken() {
        StepVerifier.create(userQueueService.generateToken("default", 1L))
                .expectNext("8b75d74c2e152f3e9566da613179c2fa9458f830d2e305a879fd1a6821fbca54")
                .verifyComplete();
    }

    @Test
    void isAllowedByToken() {
        StepVerifier.create(userQueueService.isAllowedByToken("default", 1L, "8b75d74c2e152f3e9566da613179c2fa9458f830d2e305a879fd1a6821fbca54"))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(userQueueService.isAllowedByToken("default", 1L, "invalid-token"))
                .expectNext(false)
                .verifyComplete();
    }
}