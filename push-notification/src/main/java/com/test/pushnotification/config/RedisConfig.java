package com.test.pushnotification.config;

import com.test.pushnotification.payload.RedisNotificationPayload;
import com.test.pushnotification.redis.RedisMessagePublisher;
import com.test.pushnotification.redis.RedisMessageSubscriber;
import com.test.pushnotification.service.EmitterService;
import lombok.Setter;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import redis.clients.jedis.Jedis;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties("config.redis")
public class RedisConfig {

    @Setter
    private Set<String> addresses;
    @Setter
    private Type type;
    @Setter
    private String master;

    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
        JedisClientConfiguration.JedisClientConfigurationBuilder jedisClientConfig = JedisClientConfiguration.builder();
        jedisClientConfig.connectTimeout(Duration.ofSeconds(60));
        jedisClientConfig.usePooling().poolConfig(poolConfig);
        return getRedisConfiguration(jedisClientConfig.build());
    }

    private JedisConnectionFactory getRedisConfiguration(JedisClientConfiguration jedisClientConfiguration) {
        System.out.println("Конфиг");
        System.out.println(addresses);
        System.out.println(type);
        System.out.println(master);
        switch (type) {
            case CLUSTER:
                Set<RedisNode> nodes = addresses.stream().map(this::toRedisNode).collect(Collectors.toSet());
                RedisClusterConfiguration redisClusterConfiguration = new RedisClusterConfiguration();
                redisClusterConfiguration.setClusterNodes(nodes);
                return new JedisConnectionFactory(redisClusterConfiguration, jedisClientConfiguration);
            case SENTINEL:
                RedisSentinelConfiguration redisSentinelConfiguration = new RedisSentinelConfiguration();
                redisSentinelConfiguration.master(master);
                for (String address : addresses) {
                    redisSentinelConfiguration.sentinel(toRedisNode(address));
                }
                return new JedisConnectionFactory(redisSentinelConfiguration, jedisClientConfiguration);
            case STANDALONE:
                Optional<RedisNode> optionalRedisNode = addresses.stream().map(this::toRedisNode).findFirst();
                if (optionalRedisNode.isPresent()) {
                    final RedisNode redisNode = optionalRedisNode.get();
                    RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
                    redisStandaloneConfiguration.setHostName(Objects.requireNonNull(redisNode.getHost()));
                    redisStandaloneConfiguration.setPort(Objects.requireNonNull(redisNode.getPort()));
                    return new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration);
                } else {
                    throw new NullPointerException("Не найден адрес в переменной config.redis.addresses");
                }
            default:
                // дефолт не нужен, приложение и так не запустится и даже назовёт допустимые значения
                throw new IllegalArgumentException();
        }
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        final RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(jedisConnectionFactory());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(RedisNotificationPayload.class));
        return template;
    }

    private RedisNode toRedisNode(String address) {
        String[] split = address.split(":");
        return new RedisNode(split[0], Integer.parseInt(split[1]));
    }

    @Bean
    MessageListenerAdapter messageListener(EmitterService emitterService) {
        return new MessageListenerAdapter(new RedisMessageSubscriber(emitterService));
    }

    @Bean
    RedisMessageListenerContainer redisContainer(MessageListenerAdapter messageListener) {
        final RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(jedisConnectionFactory());
        container.addMessageListener(messageListener, topic());
        return container;
    }

    @Bean
    RedisMessagePublisher redisPublisher() {
        return new RedisMessagePublisher(redisTemplate(), topic());
    }

    @Bean
    ChannelTopic topic() {
        return new ChannelTopic("pubsub:queue");
    }

    public enum Type {
        CLUSTER,
        SENTINEL,
        STANDALONE
    }

}
