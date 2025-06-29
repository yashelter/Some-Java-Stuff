package ru.mai.lessons.rpks.impl;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import ru.mai.lessons.rpks.IDirectorySizeChecker;
import ru.mai.lessons.rpks.exception.DirectoryAccessException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.testng.Assert.*;

public class DirectorySizeCheckerTest {
  private IDirectorySizeChecker directorySizeChecker;

  @BeforeClass
  public void setUp() {
    directorySizeChecker = new DirectorySizeChecker();
  }

  @DataProvider(name = "positiveCases", parallel = true)
  private Object[][] getPositiveCases() {
    return new Object[][] {
        {"cats"},
        {"dogs"}
    };
  }

  @Test(dataProvider = "positiveCases",
        description = "Проверка размера существующей непустой директории")
  public void testPositiveCheckSize(String directoryName)
      throws DirectoryAccessException {
    // WHEN
    String actualDirectorySize = directorySizeChecker.checkSize(directoryName);

    // THEN
    assertFalse(actualDirectorySize.isBlank());
  }

  @Test(description = "Проверка размера существующей пустой директории")
  public void testPositiveCheckEmptyDirectory() throws DirectoryAccessException {
    // GIVEN
    String directoryName = "rats";
    String expectedDirectorySize = "0 bytes";

    // CREATE EMPTY DIRECTORY
    try {
      Files.createDirectories(Paths.get("src/test/resources/rats"));
    } catch (IOException e) {
      fail();
    }

    // WHEN
    String actualDirectorySize = directorySizeChecker.checkSize(directoryName);

    // THEN
    assertEquals(actualDirectorySize, expectedDirectorySize);
  }

  @Test(expectedExceptions = DirectoryAccessException.class,
        description = "Проверка реакции сервиса на несуществующую директорию")
  public void testNegativeCheckUnknownDirectory() throws DirectoryAccessException {
    // GIVEN
    String directoryName = "dragons";

    // WHEN
    directorySizeChecker.checkSize(directoryName);

    // THEN ожидаем получение исключения
  }
}