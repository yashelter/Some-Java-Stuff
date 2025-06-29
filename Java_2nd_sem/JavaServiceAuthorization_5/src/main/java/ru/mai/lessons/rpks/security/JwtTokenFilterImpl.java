package ru.mai.lessons.rpks.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.mai.lessons.rpks.utils.TokenUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenFilterImpl extends OncePerRequestFilter {

  private final JwtVerifierService verifierService;

  private static final Set<String> EXCLUDED_PATHS = Set.of(
          "/register",
          "/auth/",
          "/something"
  );

  private static final String TOKEN_HEADER = "Authorization";


  @Override
  protected void doFilterInternal(
          @NonNull HttpServletRequest request,
          @NonNull HttpServletResponse response,
          @NonNull FilterChain filterChain) throws ServletException, IOException  {
    String path = request.getRequestURI();

    if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
      filterChain.doFilter(request, response);
      return;
    }

    String rawHeader = request.getHeader(TOKEN_HEADER);
    String jwt = TokenUtils.extractToken(rawHeader);

    if (jwt == null) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "jwt is null");
      return;
    }

    String user;
    try {
      user = verifierService.getUsername(jwt);
    } catch (Exception ex) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
      return;
    }

    if (!verifierService.verify(jwt)) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is invalid");
      return;
    }

    SecurityContextHolder.getContext().setAuthentication(new TokenAuthentication(user));
    filterChain.doFilter(request, response);
  }
}
