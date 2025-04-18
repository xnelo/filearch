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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestScoped
public class UserService {
  @Inject UserRepo userRepo;

  private static ServiceResponse<User> toServiceResponse(final User user) {
    return new ServiceResponse<>(User.USER_RESOURCE_TYPE, user);
  }

  public Uni<ServiceResponse<User>> getUserFromUserToken(final UserToken userToken) {
    return userRepo.getUserFromExternalId(userToken.getId()).map(UserService::toServiceResponse);
  }

  public Uni<ServiceResponse<User>> getUserById(final int userId) {
    return userRepo
        .getUserFromId(userId)
        .map(
            user -> {
              if (user == null) {
                return new ServiceResponse<>(
                    User.USER_RESOURCE_TYPE,
                    List.of(
                        ServiceError.builder()
                            .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                            .errorMessage("User doesn't exist.")
                            .httpCode(404)
                            .build()));
              } else {
                return toServiceResponse(user);
              }
            });
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

  public Uni<ServiceResponse<User>> updateUser(
      final UserContract toUpdate, final UserToken userToken) {
    if (toUpdate.getUsername() != null) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  User.USER_RESOURCE_TYPE,
                  List.of(
                      ServiceError.builder()
                          .errorCode(ErrorCode.USERNAME_CANNOT_BE_UPDATED)
                          .errorMessage("Username cannot be updated.")
                          .httpCode(400)
                          .build())));
    } else if (toUpdate.getId() != null) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  User.USER_RESOURCE_TYPE,
                  List.of(
                      ServiceError.builder()
                          .errorCode(ErrorCode.USER_ID_CANNOT_BE_UPDATED)
                          .errorMessage("User ID cannot be updated.")
                          .httpCode(400)
                          .build())));
    }

    return userRepo
        .getUserFromExternalId(userToken.getId())
        .chain(
            user -> {
              if (user == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            User.USER_RESOURCE_TYPE,
                            List.of(
                                ServiceError.builder()
                                    .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                                    .errorMessage(
                                        "The user you are trying to update does not exist.")
                                    .httpCode(404)
                                    .build())));
              }

              Map<String, Object> userUpdateMap = toUpdateMap(toUpdate);
              if (userUpdateMap.isEmpty()) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            User.USER_RESOURCE_TYPE,
                            List.of(
                                ServiceError.builder()
                                    .errorCode(ErrorCode.NO_FIELDS_TO_UPDATE)
                                    .errorMessage("No fields to update")
                                    .httpCode(400)
                                    .build())));
              }
              return userRepo
                  .updateUser(user.getId(), userUpdateMap)
                  .map(UserService::toServiceResponse);
            });
  }

  private Map<String, Object> toUpdateMap(final UserContract toUpdate) {
    Map<String, Object> updateMap = new HashMap<>();

    if (toUpdate.getEmail() != null) {
      updateMap.put(UserRepo.EMAIL_COLUMN_NAME, toUpdate.getEmail());
    }

    if (toUpdate.getLastName() != null) {
      updateMap.put(UserRepo.LAST_NAME_COLUMN_NAME, toUpdate.getLastName());
    }

    if (toUpdate.getFirstName() != null) {
      updateMap.put(UserRepo.FIRST_NAME_COLUMN_NAME, toUpdate.getFirstName());
    }

    return updateMap;
  }
}
