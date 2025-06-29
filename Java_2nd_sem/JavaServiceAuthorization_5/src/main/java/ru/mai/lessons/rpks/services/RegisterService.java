package ru.mai.lessons.rpks.services;

import ru.mai.lessons.rpks.dto.response.TokenResponse;

public interface RegisterService {

  TokenResponse register(String username);
}
