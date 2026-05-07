package backend.academy.linktracker.scrapper.config.cache;

import backend.academy.linktracker.scrapper.dto.ListLinksResponse;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.lettuce.core.resource.DnsResolvers;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class CacheConfiguration {

    @Value("${app.redis.lettuce.client.host:127.0.0.1}")
    public String host;

    @Bean(destroyMethod = "shutdown")
    public ClientResources lettuceClientResources() {
        return DefaultClientResources.builder()
            .socketAddressResolver(MappingSocketAddressResolver.create(
                DnsResolvers.JVM_DEFAULT,
                hostAndPort -> HostAndPort.of(host, hostAndPort.getPort())
            ))
            .build();
    }

    @Bean
    public RedisTemplate<String, ListLinksResponse> redisTemplate(
        RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String, ListLinksResponse> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new JacksonJsonRedisSerializer<>(ListLinksResponse.class));
        return redisTemplate;
    }

}
