package ru.mai.lessons.rpks;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import ru.mai.lessons.rpks.models.User;
import ru.mai.lessons.rpks.repositories.UserRepository;
import ru.mai.lessons.rpks.security.JwtVerifierService;
import ru.mai.lessons.rpks.services.UserService;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = {JavaServiceBffTest.Initializer.class})
public class JavaServiceBffTest {

  public static final String DEDUPLICATION_URL = "http://localhost:8081/deduplication";
  public static final String ENRICHMENT_URL = "http://localhost:8081/enrichment";
  public static final String FILTER_URL = "http://localhost:8081/filter";

  private static final Integer REDIS_PORT = 6379;
  private static final Integer POSTGRES_PORT = 5432;
  private static final Integer APPLICATION_PORT = 8080;

  private static final String USER_CACHE_NAME = "UserCache";

  private static final Boolean ENABLED_LIQUIBASE = true;

  @LocalServerPort
  private int port;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private UserService userService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private JwtVerifierService jwtVerifierService;

  @Autowired
  private CacheManager cacheManager;

  @BeforeEach
  public void setup() {
    RestAssured.port = port;
    userRepository.deleteAll();
    clearCache(USER_CACHE_NAME);
  }

  private void clearCache(String cacheName) {
    Cache userCache = cacheManager.getCache(cacheName);

    if (userCache != null) {
      userCache.clear();
    }
  }

  @Container
  public static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(
      DockerImageName.parse("postgres"))
      .withDatabaseName("test_db")
      .withUsername("username")
      .withPassword("password")
      .withReuse(true)
      .withExposedPorts(POSTGRES_PORT)
      .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
          new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(POSTGRES_PORT),
              new ExposedPort(POSTGRES_PORT)))
      ));

  @Container
  public static GenericContainer<?> redisContainer = new GenericContainer<>(
      DockerImageName.parse("redis"))
      .withExposedPorts(6379)
      .waitingFor(Wait.forListeningPort())
      .withReuse(true);

  static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
      TestPropertyValues.of(
          "spring.datasource.url=" + postgresContainer.getJdbcUrl(),
          "spring.datasource.username=" + postgresContainer.getUsername(),
          "spring.datasource.password=" + postgresContainer.getPassword(),
          "spring.liquibase.enabled=" + ENABLED_LIQUIBASE,
          "spring.data.redis.host=" + redisContainer.getHost(),
          "spring.data.redis.port=" + redisContainer.getMappedPort(REDIS_PORT),
          "feign.client.url.deduplication=" + DEDUPLICATION_URL,
          "feign.client.url.enrichment=" + ENRICHMENT_URL,
          "feign.client.url.filter=" + FILTER_URL,
          "server.port=" + APPLICATION_PORT
      ).applyTo(configurableApplicationContext.getEnvironment());
    }
  }

  @Test
  @DisplayName("Тест на корректную миграцию")
  void testTableCreatedAfterMigration() {
    String tableName = "user";
    String sql = "SELECT to_regclass('public." + tableName + "');";

    String result = jdbcTemplate.queryForObject(sql, String.class);

    assertNotNull(result, "Таблица " + tableName + " не была создана после миграции");
  }

  @Test
  @DisplayName("Тест на неуспешную аутентификацию через токен")
  void givenRequestWithNoToken_whenSomeMethod_thenReturnScUnauthorized() {
    RestAssured.given()
        .contentType(ContentType.JSON)
        .when()
        .get("/deduplication/findAll")
        .then()
        .log().all()
        .statusCode(401)
        .extract()
        .response();
  }

  @Test
  @DisplayName("Тест на неуспешную аутентификацию через токен из-за отсутствия пользователя в БД")
  void givenRequestTokenAndNoUserInDatabase_whenSomeMethod_thenReturnScUnauthorized() {
    RestAssured.given()
        .contentType(ContentType.URLENC)
        .when()
        .header("Authorization", "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJBbGV4YW5kciIsImlzcyI6InN0dWRlbnQiLCJleHAiOjMxNTU2ODg5ODY0NDAzMTk5LCJpYXQiOjE3MzcyMzUyMDR9.nwaIS1ck9ylb7YryV33HVflm0sGOGGqpvufj-dHoO7s")
        .get("/deduplication/findAll")
        .then()
        .log().all()
        .statusCode(401)
        .extract()
        .response();
  }

  @Test
  @DisplayName("Тест на успешную верификацию токена")
  void givenToken_whenVerify_thenReturnTrue() {
    String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJBbGV4YW5kciIsImlzcyI6InN0dWRlbnQiLCJleHAiOjMxNTU2ODg5ODY0NDAzMTk5LCJpYXQiOjE3MzcyMzUyMDR9.nwaIS1ck9ylb7YryV33HVflm0sGOGGqpvufj-dHoO7s";
    userRepository.saveAndFlush(User.builder().username("Alexandr").build());
    assertTrue(jwtVerifierService.verify(token));
  }

  @Test
  @DisplayName("Тест на кэширование метода loadUserByUsername")
  void givenRequest_whengetAllDeduplicationsByDeduplicationId_thenReturnAddInCache() {
    userRepository.saveAndFlush(User.builder().username("Alexandr").build());
    userService.loadUserByUsername("Alexandr");

    assertNotNull(cacheManager.getCache(USER_CACHE_NAME));
    assertNotNull(cacheManager.getCache(USER_CACHE_NAME).get("Alexandr"));
  }
}