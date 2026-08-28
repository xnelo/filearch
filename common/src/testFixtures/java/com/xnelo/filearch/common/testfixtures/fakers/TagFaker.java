package com.xnelo.filearch.common.testfixtures.fakers;

import com.github.javafaker.Faker;
import com.xnelo.filearch.common.model.Tag;

public class TagFaker {
  private static final Faker faker = new Faker();

  public static Tag generateInstance() {
    return Tag.builder()
        .id(faker.random().nextLong())
        .ownerId(faker.random().nextLong())
        .tagName(faker.funnyName().name())
        .build();
  }
}
