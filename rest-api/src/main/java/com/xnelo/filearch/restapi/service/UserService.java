package com.xnelo.filearch.restapi.service;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceError;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.common.utils.ServiceResponseUtils;
import com.xnelo.filearch.jooq.tables.Users;
import com.xnelo.filearch.restapi.api.contracts.UserContract;
import com.xnelo.filearch.restapi.data.UserRepo;
import com.xnelo.filearch.restapi.service.folder.FolderService;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestScoped
public class UserService {
  @Inject UserRepo userRepo;
  @Inject FolderService folderService;

  public Uni<ServiceResponse<User>> getUserFromUserToken(
      final ServiceRequestContext requestContext) {
    return userRepo
        .getUserFromExternalId(requestContext.getUserToken().getId())
        .map(
            user -> {
              if (user == null) {
                return new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        List.of(
                            ServiceError.builder()
                                .httpCode(404)
                                .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                                .errorMessage("User does not exist in Database.")
                                .build())));
              } else {
                return new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(), requestContext.getActionType(), user));
              }
            });
  }

  public Uni<ServiceResponse<User>> getUserById(
      final ServiceRequestContext requestContext, final int userId) {
    return userRepo
        .getUserFromId(userId)
        .map(
            user -> {
              if (user == null) {
                return new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        List.of(
                            ServiceError.builder()
                                .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                                .errorMessage("User doesn't exist.")
                                .httpCode(404)
                                .build())));
              } else {
                return new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(), requestContext.getActionType(), user));
              }
            });
  }

  private User createUserObject(
      final ServiceRequestContext requestContext, final UserContract inputData) {
    User.UserBuilder builder =
        User.builder()
            .externalId(requestContext.getUserToken().getId())
            .username(inputData.getUsername());

    if (inputData.getFirstName() == null || inputData.getLastName() == null) {
      builder
          .firstName(requestContext.getUserToken().getFirstName())
          .lastName(requestContext.getUserToken().getLastName());
    } else {
      builder.firstName(inputData.getFirstName()).lastName(inputData.getLastName());
    }

    if (inputData.getEmail() == null) {
      builder.email(requestContext.getUserToken().getEmail());
    } else {
      builder.email(inputData.getEmail());
    }

    return builder.build();
  }

  public Uni<ServiceResponse<User>> createUser(
      final ServiceRequestContext requestContext, final UserContract inputData) {
    return userRepo
        .isUsernameUnique(inputData.getUsername())
        .chain(
            (isUsernameUnique) -> {
              if (!isUsernameUnique) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                requestContext.getResourceType(),
                                requestContext.getActionType(),
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.USERNAME_MUST_BE_UNIQUE)
                                        .errorMessage(
                                            "Username '"
                                                + inputData.getUsername()
                                                + "' is not unique.")
                                        .httpCode(400)
                                        .build()))));
              } else {
                return createUserIfNotExist(requestContext, inputData);
              }
            });
  }

  private Uni<ServiceResponse<User>> createUserIfNotExist(
      final ServiceRequestContext requestContext, final UserContract inputData) {
    return userRepo
        .getUserFromExternalId(requestContext.getUserToken().getId())
        .chain(
            existingUser -> {
              if (existingUser != null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                requestContext.getResourceType(),
                                requestContext.getActionType(),
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.USER_ALREADY_EXISTS)
                                        .errorMessage(
                                            "An account already exists for this user. You cannot create another.")
                                        .httpCode(400)
                                        .build()))));
              } else {
                return userRepo
                    .createNewUser(createUserObject(requestContext, inputData))
                    .chain(this::createRootFolder)
                    .map(
                        user ->
                            new ServiceResponse<>(
                                new ServiceActionResponse<>(
                                    requestContext.getResourceType(),
                                    requestContext.getActionType(),
                                    user)));
              }
            });
  }

  Uni<User> createRootFolder(final User user) {
    return folderService
        .createRootFolderForUser(user.getId())
        .chain(
            folder ->
                userRepo.updateUser(
                    user.getId(), Map.of(Users.USERS.ROOT_FOLDER_ID.getName(), folder.getId())));
  }

  public Uni<ServiceResponse<User>> updateUser(
      final ServiceRequestContext requestContext, final UserContract toUpdate) {
    if (toUpdate.getUsername() != null) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      requestContext.getResourceType(),
                      requestContext.getActionType(),
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.USERNAME_CANNOT_BE_UPDATED)
                              .errorMessage("Username cannot be updated.")
                              .httpCode(400)
                              .build()))));
    } else if (toUpdate.getId() != null) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      requestContext.getResourceType(),
                      requestContext.getActionType(),
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.USER_ID_CANNOT_BE_UPDATED)
                              .errorMessage("User ID cannot be updated.")
                              .httpCode(400)
                              .build()))));
    } else if (toUpdate.getRootFolderId() != null) {
      return Uni.createFrom()
          .item(
              new ServiceResponse<>(
                  new ServiceActionResponse<>(
                      requestContext.getResourceType(),
                      requestContext.getActionType(),
                      List.of(
                          ServiceError.builder()
                              .errorCode(ErrorCode.USER_ROOT_FOLDER_ID_CANNOT_BE_UPDATED)
                              .errorMessage("User Root Folder Id cannot be updated.")
                              .httpCode(400)
                              .build()))));
    }

    return userRepo
        .getUserFromExternalId(requestContext.getUserToken().getId())
        .chain(
            user -> {
              if (user == null) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                requestContext.getResourceType(),
                                requestContext.getActionType(),
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.USER_DOES_NOT_EXIST)
                                        .errorMessage(
                                            "The user you are trying to update does not exist.")
                                        .httpCode(404)
                                        .build()))));
              }

              Map<String, Object> userUpdateMap = toUpdateMap(toUpdate);
              if (userUpdateMap.isEmpty()) {
                return Uni.createFrom()
                    .item(
                        new ServiceResponse<>(
                            new ServiceActionResponse<>(
                                requestContext.getResourceType(),
                                requestContext.getActionType(),
                                List.of(
                                    ServiceError.builder()
                                        .errorCode(ErrorCode.NO_FIELDS_TO_UPDATE)
                                        .errorMessage("No fields to update")
                                        .httpCode(400)
                                        .build()))));
              }
              return userRepo
                  .updateUser(user.getId(), userUpdateMap)
                  .map(
                      userReturn ->
                          new ServiceResponse<>(
                              new ServiceActionResponse<>(
                                  requestContext.getResourceType(),
                                  requestContext.getActionType(),
                                  userReturn)));
            });
  }

  public Uni<ServiceRequestContext> checkUserExist(ServiceRequestContext requestContext) {
    return userRepo
        .getUserFromExternalId(requestContext.getUserToken().getId())
        .map(
            user -> {
              if (user == null) {
                throw new ServiceResponseException(
                    requestContext, ErrorCode.USER_DOES_NOT_EXIST, "User does not exist", 404);
              }

              requestContext.setUser(user);

              return requestContext;
            });
  }

  public Uni<ServiceResponse<User>> deleteUser(final ServiceRequestContext requestContext) {
    return checkUserExist(requestContext)
        .chain(
            context ->
                folderService
                    .deleteRootFolder(context, context.getUser().getRootFolderId())
                    // TODO: Check if we need to delete groups, tags, permissions, etc.
                    .chain(
                        deleteFolderResponse -> {
                          if (deleteFolderResponse.hasError()) {
                            return Uni.createFrom()
                                .item(
                                    ServiceResponseUtils.updateErrorAndPassThrough(
                                        deleteFolderResponse, ResourceType.USER));
                          }

                          return userRepo
                              .deleteUser(context.getUser().getId())
                              .map(
                                  deletedUser -> {
                                    if (deletedUser == null) {
                                      return new ServiceResponse<>(
                                          new ServiceActionResponse<>(
                                              context.getResourceType(),
                                              context.getActionType(),
                                              List.of(
                                                  ServiceError.builder()
                                                      .errorCode(ErrorCode.USER_DELETE_ERROR)
                                                      .errorMessage(
                                                          "Error deleting user from database.")
                                                      .httpCode(500)
                                                      .build())));
                                    }

                                    return new ServiceResponse<>(
                                        new ServiceActionResponse<>(
                                            context.getResourceType(),
                                            context.getActionType(),
                                            deletedUser));
                                  });
                        }));
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

  public Uni<ServiceResponse<Boolean>> isUsernameAvailable(
      final ServiceRequestContext requestContext, final String username) {
    return userRepo
        .isUsernameUnique(username)
        .map(
            isUsernameUnique ->
                new ServiceResponse<>(
                    new ServiceActionResponse<>(
                        requestContext.getResourceType(),
                        requestContext.getActionType(),
                        isUsernameUnique)));
  }

  Uni<User> getUserByUsername(final ServiceRequestContext requestContext, final String username) {
    return userRepo
        .getUserFromUsername(username)
        .invoke(
            user -> {
              if (user == null) {
                throw new ServiceResponseException(
                    requestContext,
                    ErrorCode.USER_DOES_NOT_EXIST,
                    "User with username '" + username + "' does not exist",
                    404);
              }
            });
  }
}
