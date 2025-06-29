package ru.mai.lessons.rpks.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.mai.lessons.rpks.models.User;
import ru.mai.lessons.rpks.services.UserService;

import java.time.Duration;
import java.util.Optional;

import static ru.mai.lessons.rpks.utils.TokenUtils.getSubject;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtVerifierService {

  private final UserService userService;

  private JWTVerifier verifier;

  @Value("${token.signing.secret}")
  private String secret;

  @Value("${token.issuer}")
  private String issuer;

  public JWTVerifier getVerifier() {
    if (verifier == null) {
      verifier = JWT
              .require(Algorithm.HMAC256(secret))
              .withIssuer(issuer)
              .build();
    }
    return verifier;
  }

  public String getUsername(String token){
    return getSubject(getVerifier(), token);
  }

  public boolean verify(String token) {
    if (token == null || token.isBlank()) {
      log.error("Token is null or empty");
      return false;
    }

    try {
      String username = getSubject(getVerifier(), token);
      Optional<User> user = userService.loadUserByUsername(username);
      if (user.isEmpty()) {
        log.error("User not found");
        return false;
      }

      log.info("Token verified successfully for user: {}", username);
      return true;

    } catch (Exception ex) {
      log.error("Unexpected error during JWT verification", ex);
      return false;
    }
  }
}


