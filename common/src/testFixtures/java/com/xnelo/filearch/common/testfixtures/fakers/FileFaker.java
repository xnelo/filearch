package com.xnelo.filearch.common.testfixtures.fakers;

import com.github.javafaker.Faker;
import com.xnelo.filearch.common.model.File;
import com.xnelo.filearch.common.model.StorageType;

public class FileFaker {
  private static final Faker faker = new Faker();

  public static File generateInstance() {
    return File.builder()
        .id(faker.random().nextLong())
        .ownerId(faker.random().nextLong())
        .folderId(faker.random().nextLong())
        .storageType(faker.options().option(StorageType.class))
        .storageKey(faker.file().fileName())
        .originalFilename(faker.file().fileName())
        .mimeType(faker.file().mimeType())
        .build();
  }
}
