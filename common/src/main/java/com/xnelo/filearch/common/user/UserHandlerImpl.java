package com.xnelo.filearch.common.user;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

@RequestScoped
public class UserHandlerImpl implements UserHandler {
  @Inject JsonWebToken jwt;

  private User user;

  @Override
  public User getUserInfo() {
    if (user == null) {
      user = createNewUser(jwt);
    }
    return user;
  }

  private static User createNewUser(JsonWebToken token) {
    return UserImpl.builder().id(token.getSubject()).build();
  }
}
