package ru.mai.lessons.rpks.utils;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.context.SecurityContextHolder;

@UtilityClass
public class SecurityContextUtils {

  public String getCurrentUser() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
