package ru.mai.lessons.rpks;

import ru.mai.lessons.rpks.exception.DirectoryAccessException;

public interface IDirectorySizeChecker {
  public String checkSize(String directoryName) throws DirectoryAccessException; // запускает проверку размера директории
}
