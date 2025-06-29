package ru.mai.lessons.rpks.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.mai.lessons.rpks.models.User;
import ru.mai.lessons.rpks.repositories.UserRepository;
import ru.mai.lessons.rpks.services.UserService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;

  @Override
  public User createUser(User user) {
    return userRepository.save(user);
  }

  @Override
  @Cacheable(value = "UserCache", unless = "#result == null")
  public Optional<User> loadUserByUsername(String username) {
   return userRepository.findByUsername(username);
  }
}
