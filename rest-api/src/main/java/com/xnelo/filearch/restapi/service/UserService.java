package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.restapi.api.contracts.UserContract;
import com.xnelo.filearch.restapi.data.UserRepo;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.List;

@RequestScoped
public class UserService {
  @Inject UserRepo userRepo;

  private static ServiceResponse<User> toServiceResponse(final User user) {
    return new ServiceResponse<>(User.USER_RESOURCE_TYPE, user);
  }

  public Uni<ServiceResponse<User>> getUserFromUserToken(final UserToken userToken) {
    return userRepo.getUserFromExternalId(userToken.getId()).map(UserService::toServiceResponse);
  }

  private User createUserObject(final UserContract inputData, final UserToken token) {
    User.UserBuilder builder =
        User.builder().externalId(token.getId()).username(inputData.getUsername());

    if (inputData.getFirstName() == null || inputData.getLastName() == null) {
      builder.firstName(token.getFirstName()).lastName(token.getLastName());
    } else {
      builder.firstName(inputData.getFirstName()).lastName(inputData.getLastName());
    }

    if (inputData.getEmail() == null) {
      builder.email(token.getEmail());
    } else {
      builder.email(inputData.getEmail());
    }

    return builder.build();
  }

  public Uni<ServiceResponse<User>> createUser(
      final UserContract inputData, final UserToken token) {
    return userRepo
        .isUsernameUnique(inputData.getUsername())
        .chain(
            (isUsernameUnique) -> {
              if (!isUsernameUnique) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            User.USER_RESOURCE_TYPE,
                            List.of(
                                ServiceError.builder()
                                    .errorCode(ErrorCode.USERNAME_MUST_BE_UNIQUE)
                                    .errorMessage(
                                        "Username '" + inputData.getUsername() + "' is not unique.")
                                    .httpCode(400)
                                    .build())));
              } else {
                return createUserIfNotExist(inputData, token);
              }
            });
  }

  private Uni<ServiceResponse<User>> createUserIfNotExist(
      final UserContract inputData, final UserToken token) {
    return userRepo
        .getUserFromExternalId(token.getId())
        .chain(
            existingUser -> {
              if (existingUser != null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            User.USER_RESOURCE_TYPE,
                            List.of(
                                ServiceError.builder()
                                    .errorCode(ErrorCode.USER_ALREADY_EXISTS)
                                    .errorMessage(
                                        "An account already exists for this user. You cannot create another.")
                                    .httpCode(400)
                                    .build())));
              } else {
                return userRepo
                    .createNewUser(createUserObject(inputData, token))
                    .map(UserService::toServiceResponse);
              }
            });
  }
}
