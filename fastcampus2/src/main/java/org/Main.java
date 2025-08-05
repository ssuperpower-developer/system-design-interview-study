package org;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");

        try (var jedisPool = new JedisPool("localhost", 6379)) {
            try (Jedis jedis = jedisPool.getResource()) {
                // string
//                jedis.set("users:300:email", "hi@naver.com");
//                jedis.set("users:300:name", "kim");
//                jedis.set("users:300:age", "25");
//
//                String userEmail = jedis.get("users:300:email");
//                System.out.println(userEmail);
//
//                List<String> userInfo = jedis.mget("users:300:email", "users:300:name", "users:300:age");
//                userInfo.forEach(System.out::println);
//
//                long counter = jedis.incr("counter");
//                System.out.println(counter);
//
//                counter = jedis.incrBy("counter", 10L);
//                System.out.println(counter);
//
//                counter = jedis.decr("counter");
//                System.out.println(counter);
//
//                counter = jedis.decrBy("counter", 10L);
//                System.out.println(counter);
//
//                Pipeline pipeline = jedis.pipelined();
//                pipeline.set("users:400:email", "hi@naver.com");
//                pipeline.set("users:400:name", "kim");
//                pipeline.set("users:400:age", "25");
//                List<Object> objects = pipeline.syncAndReturnAll();
//                objects.forEach(i -> System.out.println(i.toString()));

                // list
                // 1. stack
//                jedis.rpush("stack1", "aaaa");
//                jedis.rpush("stack1", "bbbb");
//                jedis.rpush("stack1", "cccc");
//
//                List<String> stack1 = jedis.lrange("stack1", 0, -1);
//                stack1.forEach(System.out::println);
//
//                while(true) {
//                    List<String> blpop = jedis.blpop(10, "queue:blocking");
//                    if(blpop != null) {
//                        blpop.forEach(System.out::println);
//                    }
//                }

                // set
//                jedis.sadd("users:500:follow", "100", "200", "300");
//                jedis.srem("users:500:follow", "100");
//
//                Set<String> members = jedis.smembers("users:500:follow");
//                members.forEach(System.out::println);
//
//                System.out.println(jedis.sismember("users:500:follow", "200"));
//                System.out.println(jedis.sismember("users:500:follow", "120"));
//
//                System.out.println(jedis.scard("users:500:follow"));
//
//                Set<String> sinter = jedis.sinter("users:500:follow", "users:100:follow");
//                sinter.forEach(System.out::println);

                // hash
                jedis.hset("users:2:info", "name", "kim");
                var userInfo = new HashMap<String, String>();
                userInfo.put("email", "hi@naver.com");
                userInfo.put("phone", "010-XXXX-XXXX");

                jedis.hset("users:2:info", userInfo);

                jedis.hdel("users:2:info", "phone");

                System.out.println(jedis.hget("users:2:info", "email"));
                Map<String,String> user2Info = jedis.hgetAll("users:2:info");
                user2Info.forEach((k, v) -> System.out.println("key:" + k + ",value:" + v));

                jedis.hincrBy("users:2:info", "visits", 1);

            }
        }
    }
}