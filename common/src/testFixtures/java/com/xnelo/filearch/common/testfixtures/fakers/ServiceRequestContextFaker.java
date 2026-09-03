package com.xnelo.filearch.common.testfixtures.fakers;

import com.github.javafaker.Faker;
import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.common.service.context.ServiceRequestContextImpl;
import com.xnelo.filearch.common.usertoken.UserToken;

public class ServiceRequestContextFaker {
  private static final Faker faker = new Faker();

  public static ServiceRequestContext generateInstance() {
    UserToken token = UserTokenFaker.generateInstance();
    User user =
        User.builder()
            .id(faker.random().nextLong())
            .externalId(token.getId())
            .username(token.getUsername())
            .firstName(token.getFirstName())
            .lastName(token.getLastName())
            .email(token.getEmail())
            .rootFolderId(faker.random().nextLong())
            .build();
    return generateInstance(token, user, faker.random().nextLong());
  }

  public static ServiceRequestContext generateInstanceNoUser() {
    UserToken token = UserTokenFaker.generateInstance();
    return generateInstance(token, null, faker.random().nextLong());
  }

  public static ServiceRequestContext generateInstance(
      final UserToken userToken, final User user, final Long groupId) {
    return ServiceRequestContextImpl.builder()
        .resourceType(faker.options().option(ResourceType.class))
        .actionType(faker.options().option(ActionType.class))
        .userToken(userToken)
        .user(user)
        .groupId(groupId)
        .build();
  }
}
