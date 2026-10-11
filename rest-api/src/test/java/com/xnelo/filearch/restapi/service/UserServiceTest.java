package com.xnelo.filearch.restapi.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.common.service.context.ServiceRequestContextImpl;
import com.xnelo.filearch.common.testfixtures.fakers.ServiceRequestContextFaker;
import com.xnelo.filearch.common.testfixtures.fakers.UserFaker;
import com.xnelo.filearch.common.testfixtures.fakers.UserTokenFaker;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.restapi.data.UserRepo;
import com.xnelo.filearch.restapi.service.folder.FolderService;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class UserServiceTest {
  UserRepo mockUserRepo;
  FolderService mockFolderService;

  UserService userService;

  @BeforeEach
  public void setup() {
    mockUserRepo = mock(UserRepo.class);
    mockFolderService = mock(FolderService.class);

    userService = new UserService(mockUserRepo, mockFolderService);
  }

  @Nested
  class CheckUserExistTests {
    @Test
    void userNullThrowsException() {
      ServiceRequestContext requestContext = ServiceRequestContextFaker.generateInstance();

      when(mockUserRepo.getUserFromExternalId(anyString())).thenReturn(Uni.createFrom().nullItem());

      ServiceResponseException ex =
          assertThrows(
              ServiceResponseException.class,
              () -> userService.checkUserExist(requestContext).await().indefinitely());

      assertEquals(ErrorCode.USER_DOES_NOT_EXIST, ex.getErrorCode());
    }

    @Test
    void userPresentSuccess() {
      UserToken userToken = UserTokenFaker.generateInstance();
      User user = UserFaker.from(userToken);

      when(mockUserRepo.getUserFromExternalId(anyString())).thenReturn(Uni.createFrom().item(user));

      ServiceRequestContext requestContext =
          ServiceRequestContextImpl.builder().userToken(userToken).build();

      ServiceRequestContext res =
          assertDoesNotThrow(
              () -> userService.checkUserExist(requestContext).await().indefinitely());

      assertSame(res, requestContext);
      assertEquals(user, res.getUser());
    }
  }
}
