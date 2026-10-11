package com.xnelo.filearch.common.testfixtures.fakers;

import com.github.javafaker.Faker;
import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.common.usertoken.UserToken;

public class UserFaker {
  private static final Faker faker = new Faker();

  public static User from(UserToken userToken) {
    return User.builder()
        .id(faker.random().nextLong())
        .externalId(userToken.getId())
        .username(userToken.getUsername())
        .firstName(userToken.getFirstName())
        .lastName(userToken.getLastName())
        .email(userToken.getEmail())
        .rootFolderId(faker.random().nextLong())
        .build();
  }

  public static User generateInstance() {
    return User.builder()
        .id(faker.random().nextLong())
        .externalId(faker.bothify("??#FVW##?#"))
        .username(faker.name().username())
        .firstName(faker.name().firstName())
        .lastName(faker.name().lastName())
        .email(faker.internet().emailAddress())
        .rootFolderId(faker.random().nextLong())
        .build();
  }
}
