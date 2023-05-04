package com.media.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        //指定编码，默认编码为org.redisson.codec.JsonJacksonCodec
        //config.setCodec(new org.redisson.client.codec.StringCodec());
        config.useSingleServer()
                .setAddress("redis://192.168.44.128:6379").setPassword("123456")
                .setConnectionPoolSize(50)
                .setIdleConnectionTimeout(10000)
                .setConnectTimeout(3000)
                .setTimeout(3000)
                .setDatabase(5);

        return Redisson.create(config);
    }
}
