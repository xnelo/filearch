package com.xnelo.filearch.common.usertoken;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.jwt.JsonWebToken;

@RequestScoped
public class UserTokenHandlerImpl implements UserTokenHandler {
  @Inject JsonWebToken jwt;

  private UserToken userToken;

  @Override
  public UserToken getUserInfo() {
    if (userToken == null) {
      userToken = createNewUser(jwt);
    }
    return userToken;
  }

  private static UserToken createNewUser(JsonWebToken token) {
    return UserTokenImpl.builder().id(token.getSubject()).build();
  }
}
