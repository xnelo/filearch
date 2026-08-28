package com.xnelo.filearch.common.testfixtures.fakers;

import com.github.javafaker.Faker;
import com.xnelo.filearch.common.model.Folder;

public class FolderFaker {
  private static final Faker faker = new Faker();

  public static Folder generateInstance() {
    return Folder.builder()
        .id(faker.random().nextLong())
        .ownerId(faker.random().nextLong())
        .parentId(faker.random().nextLong())
        .folderName(faker.funnyName().name())
        .build();
  }
}
