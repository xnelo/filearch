package com.xnelo.filearch.common.testfixtures.fakers;

import com.github.javafaker.Faker;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.usertoken.UserTokenImpl;
import java.util.UUID;

public class UserTokenFaker {
  private static final Faker faker = new Faker();

  public static UserToken generateInstance() {
    return UserTokenImpl.builder()
        .id(UUID.randomUUID().toString())
        .firstName(faker.name().firstName())
        .lastName(faker.name().lastName())
        .username(faker.name().username())
        .email(faker.internet().emailAddress())
        .build();
  }
}
