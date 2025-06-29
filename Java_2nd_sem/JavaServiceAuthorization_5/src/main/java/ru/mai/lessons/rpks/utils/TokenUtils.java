package ru.mai.lessons.rpks.utils;

import com.auth0.jwt.JWTVerifier;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import ru.mai.lessons.rpks.exception.ParseTokenException;

@UtilityClass
public class TokenUtils {

  private static final String TOKEN_PREFIX = "Bearer ";


  public static String getSubject(JWTVerifier verifier, String token) {
    return verifier.verify(token).getSubject();
  }

  public static String extractToken(String header) {
    if (StringUtils.isBlank(header) || !header.startsWith(TOKEN_PREFIX)) {
      return null;
    }

    try {
      return header.substring(TOKEN_PREFIX.length());
    } catch (Exception ex) {
      throw new ParseTokenException("Invalid token");
    }
  }
}
