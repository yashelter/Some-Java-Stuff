package ru.mai.lessons.rpks.services;

import ru.mai.lessons.rpks.models.User;

import java.util.Optional;

public interface UserService {

  User createUser(User user);

  Optional<User> loadUserByUsername(String username);
}
