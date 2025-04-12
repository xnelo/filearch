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
    UserTokenImpl.UserTokenImplBuilder builder = UserTokenImpl.builder();

    builder.id(token.getSubject());

    if (token.getClaim("given_name") != null && token.getClaim("family_name") != null) {
      builder.firstName(token.getClaim("given_name")).lastName(token.getClaim("family_name"));
    }

    if (token.getClaim("preferred_username") != null) {
      builder.username(token.getClaim("preferred_username"));
    }

    if (token.getClaim("email") != null) {
      builder.email(token.getClaim("email"));
    }

    return builder.build();
  }
}
