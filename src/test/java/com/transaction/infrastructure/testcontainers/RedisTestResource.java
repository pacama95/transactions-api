package com.transaction.infrastructure.testcontainers;

import com.redis.testcontainers.RedisContainer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

/**
 * Quarkus test resource that manages a Redis test container lifecycle.
 * This resource starts a Redis container before tests run and stops it after tests complete.
 */
public class RedisTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String REDIS_IMAGE = "redis:7-alpine";

    private RedisContainer redisContainer;

    @Override
    public Map<String, String> start() {
        // Create and start Redis container
        redisContainer = new RedisContainer(DockerImageName.parse(REDIS_IMAGE))
                .withReuse(false); // Don't reuse containers between test runs

        redisContainer.start();

        // Return configuration properties for Quarkus to use
        Map<String, String> config = new HashMap<>();

        // Redis configuration
        String redisUrl = String.format("redis://%s:%d",
                redisContainer.getHost(),
                redisContainer.getFirstMappedPort());

        config.put("quarkus.redis.hosts", redisUrl);
        config.put("quarkus.redis.timeout", "10s");
        config.put("quarkus.redis.max-pool-size", "20");
        config.put("quarkus.redis.max-pool-waiting", "50");

        System.out.println("Redis Test Container started: " + redisUrl);

        return config;
    }

    @Override
    public void stop() {
        if (redisContainer != null && redisContainer.isRunning()) {
            redisContainer.stop();
            System.out.println("Redis Test Container stopped");
        }
    }

    /**
     * Get the running Redis container instance.
     * Can be used by tests to interact with the container directly if needed.
     */
    public RedisContainer getRedisContainer() {
        return redisContainer;
    }
}
