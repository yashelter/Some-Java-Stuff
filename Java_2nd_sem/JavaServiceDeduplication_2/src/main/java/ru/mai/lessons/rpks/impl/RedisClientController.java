package ru.mai.lessons.rpks.impl;

import com.typesafe.config.Config;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.exceptions.JedisException;
import ru.mai.lessons.rpks.RedisClient;

import java.util.Objects;

@Slf4j
public class RedisClientController implements RedisClient, AutoCloseable {
    private static final String REDIS_HOST_CONFIG = "redis.host";
    private static final String REDIS_PORT_CONFIG = "redis.port";

    private static final String DUMMY_VALUE = "1";

    private final JedisPooled jedisPool;
    private final String redisHost;
    private final int redisPort;

    public RedisClientController(Config config) {
        this.redisHost = config.getString(REDIS_HOST_CONFIG);
        this.redisPort = config.getInt(REDIS_PORT_CONFIG);
        this.jedisPool = new JedisPooled(redisHost, redisPort);

        log.info("RedisClientController initialized. Connecting to Redis at {}:{}", redisHost, redisPort);
    }


    @Override
    public boolean tryInsertInDB(String key, Long lifeTime) {
        if (key == null || key.trim().isEmpty()) {
            log.warn("Invalid key");
            return false;
        }
        if (lifeTime == null || lifeTime <= 0) {
            log.warn("Invalid lifeTime '{}", lifeTime);
            return false;
        }

        log.debug("Attempting atomic set in Redis");

        try {
            SetParams setParams = SetParams.setParams().nx().ex(lifeTime);

            String result = jedisPool.set(key, DUMMY_VALUE, setParams);

            if ("OK".equals(result)) {
                log.debug("Set successfully. Allowing use");
                return true;
            } else {
                log.debug("Key '{}' already exists. Denying use.", key);
                return false;
            }
        } catch (JedisException e) {
            log.error("Redis command failed for key '{}'. Denying passage. Error: {}", key, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during Redis operation for key '{}'. Denying passage. Error: {}", key, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void close() {
        if (jedisPool != null) {
            log.info("Closing Redis connection pool for {}:{}", redisHost, redisPort);
            jedisPool.close();
        }
    }
}