package ru.mai.lessons.rpks.services.impl;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mai.lessons.rpks.dto.response.TokenResponse;
import ru.mai.lessons.rpks.models.User;
import ru.mai.lessons.rpks.services.RegisterService;
import ru.mai.lessons.rpks.services.UserService;
import org.springframework.beans.factory.annotation.Value;


import java.util.Date;


@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {
  private final UserService userService;

  @Value("${token.signing.secret}")
  private String secret;

  @Value("${token.issuer}")
  private String issuer;

  @Override
  public TokenResponse register(String username) {
    if (userService.loadUserByUsername(username).isPresent()) {
      throw new IllegalArgumentException("User with name " + username + " already exists");
    }

    userService.createUser(User.builder().username(username).build());

    String token = JWT.create()
            .withSubject(username)
            .withIssuer(issuer)
            .withIssuedAt(new Date())
            .sign(Algorithm.HMAC256(secret));

    return new TokenResponse(token);
  }
}
