package com.example.jediscache;

import com.example.jediscache.domain.entity.User;
import com.example.jediscache.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@RequiredArgsConstructor
@EnableJpaAuditing
public class JediscacheApplication implements ApplicationRunner {

    private final UserRepository userRepository;

    public static void main(String[] args) {
        SpringApplication.run(JediscacheApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        userRepository.save(
                User.builder()
                        .name("kim")
                        .email("kim@naver.com")
                        .build()
        );
        userRepository.save(
                User.builder()
                        .name("hi")
                        .email("hi@naver.com")
                        .build()
        );
        userRepository.save(
                User.builder()
                        .name("hihi")
                        .email("hihi@naver.com")
                        .build()
        );
        userRepository.save(
                User.builder()
                        .name("hihihi")
                        .email("hihihi@naver.com")
                        .build()
        );
    }
}
