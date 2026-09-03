package com.xnelo.filearch.restapi.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.GroupItemType;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.common.testfixtures.fakers.FileFaker;
import com.xnelo.filearch.common.testfixtures.fakers.FolderFaker;
import com.xnelo.filearch.common.testfixtures.fakers.ServiceRequestContextFaker;
import com.xnelo.filearch.restapi.data.FolderRepo;
import com.xnelo.filearch.restapi.data.GroupItemsRepo;
import com.xnelo.filearch.restapi.data.StoredFilesRepo;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class GroupItemServiceTest {

  private FolderRepo folderRepoMock;
  private GroupItemsRepo groupItemsRepoMock;
  private StoredFilesRepo storedFilesRepoMock;
  private GroupItemService groupItemService;

  @BeforeEach
  void setUp() {
    folderRepoMock = mock(FolderRepo.class);
    groupItemsRepoMock = mock(GroupItemsRepo.class);
    storedFilesRepoMock = mock(StoredFilesRepo.class);

    groupItemService =
        new GroupItemService(folderRepoMock, groupItemsRepoMock, storedFilesRepoMock);
  }

  @AfterEach
  void cleanup() {
    groupItemService = null;

    storedFilesRepoMock = null;
    groupItemsRepoMock = null;
    folderRepoMock = null;
  }

  @Nested
  class CheckItemExistsThrowErrorTests {
    @Test
    void itemNotExist_ThrowsItemDoesNotExistError() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      when(storedFilesRepoMock.getStoredFile(anyLong(), anyLong()))
          .thenReturn(Uni.createFrom().nullItem());

      // ACT / ASSERT
      ServiceResponseException ex =
          assertThrows(
              ServiceResponseException.class,
              () ->
                  groupItemService
                      .checkItemExistsThrowError(context, GroupItemType.FILE, 1)
                      .await()
                      .indefinitely());

      assertEquals(ErrorCode.ITEM_DOES_NOT_EXIST, ex.getErrorCode());
      assertEquals("Item ([" + GroupItemType.FILE + "] 1) does not exist.", ex.getMessage());
    }

    @Test
    void itemExist_ReturnsContext() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      when(storedFilesRepoMock.getStoredFile(anyLong(), anyLong()))
          .thenReturn(Uni.createFrom().item(FileFaker.generateInstance()));

      // ACT / ASSERT
      assertDoesNotThrow(
          () ->
              groupItemService
                  .checkItemExistsThrowError(context, GroupItemType.FILE, 1)
                  .await()
                  .indefinitely());
    }
  }

  @Nested
  class CheckItemExistsTests {
    @Test
    void itemTypeNull_ThrowsInvalidInputError() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      // ACT / ASSERT
      ServiceResponseException ex =
          assertThrows(
              ServiceResponseException.class,
              () -> groupItemService.checkItemExists(context, null, 1));

      assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
      assertEquals("Item type cannot be null", ex.getMessage());
    }

    @Test
    void itemIdNegative_ThrowsInvalidInputError() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      // ACT / ASSERT
      ServiceResponseException ex =
          assertThrows(
              ServiceResponseException.class,
              () -> groupItemService.checkItemExists(context, GroupItemType.FILE, -1L));

      assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
      assertEquals("Item ID cannot be negative", ex.getMessage());
    }

    @Test
    void noUserInContext_ThrowsInvalidInputError() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstanceNoUser();

      // ACT / ASSERT
      ServiceResponseException ex =
          assertThrows(
              ServiceResponseException.class,
              () -> groupItemService.checkItemExists(context, GroupItemType.FILE, 1L));

      assertEquals(ErrorCode.USER_NOT_PROVIDED_IN_REQUEST_OBJECT, ex.getErrorCode());
      assertEquals("User not provided in request object. Contact support.", ex.getMessage());
    }

    @Test
    void unknownItemType_ThrowsInvalidInputError() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      // ACT / ASSERT
      ServiceResponseException ex =
          assertThrows(
              ServiceResponseException.class,
              () -> groupItemService.checkItemExists(context, GroupItemType.UNKNOWN, 1L));

      assertEquals(ErrorCode.INVALID_INPUT_VALUE, ex.getErrorCode());
      assertEquals("Item type cannot be UNKNOWN", ex.getMessage());
    }

    @Test
    void fileExist_ReturnsTrue() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      when(storedFilesRepoMock.getStoredFile(anyLong(), anyLong()))
          .thenReturn(Uni.createFrom().item(FileFaker.generateInstance()));

      // ACT
      boolean result =
          groupItemService.checkItemExists(context, GroupItemType.FILE, 1).await().indefinitely();

      // ASSERT
      assertTrue(result);
    }

    @Test
    void fileNotExist_ReturnsFalse() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      when(storedFilesRepoMock.getStoredFile(anyLong(), anyLong()))
          .thenReturn(Uni.createFrom().nullItem());

      // ACT
      boolean result =
          groupItemService.checkItemExists(context, GroupItemType.FILE, 1).await().indefinitely();

      // ASSERT
      assertFalse(result);
    }

    @Test
    void getFileException_ReturnsFalse() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      when(storedFilesRepoMock.getStoredFile(anyLong(), anyLong()))
          .thenReturn(Uni.createFrom().failure(new RuntimeException("TEST EXCEPTION")));

      // ACT
      boolean result =
          groupItemService.checkItemExists(context, GroupItemType.FILE, 1).await().indefinitely();

      // ASSERT
      assertFalse(result);
    }

    @Test
    void folderExist_ReturnsTrue() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      when(folderRepoMock.getFolderById(anyLong(), anyLong()))
          .thenReturn(Uni.createFrom().item(FolderFaker.generateInstance()));

      // ACT
      boolean result =
          groupItemService.checkItemExists(context, GroupItemType.FOLDER, 1).await().indefinitely();

      // ASSERT
      assertTrue(result);
    }

    @Test
    void folderNotExist_ReturnsFalse() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      when(folderRepoMock.getFolderById(anyLong(), anyLong()))
          .thenReturn(Uni.createFrom().nullItem());

      // ACT
      boolean result =
          groupItemService.checkItemExists(context, GroupItemType.FOLDER, 1).await().indefinitely();

      // ASSERT
      assertFalse(result);
    }

    @Test
    void getFolderException_ReturnsFalse() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      when(folderRepoMock.getFolderById(anyLong(), anyLong()))
          .thenReturn(Uni.createFrom().failure(new RuntimeException("TEST EXCEPTION")));

      // ACT
      boolean result =
          groupItemService.checkItemExists(context, GroupItemType.FOLDER, 1).await().indefinitely();

      // ASSERT
      assertFalse(result);
    }
  }

  @Nested
  class CheckItemNotInGroupTests {
    @Test
    void itemInGroup_ThrowsException() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      when(groupItemsRepoMock.isItemInGroup(anyLong(), any(GroupItemType.class), anyLong()))
          .thenReturn(Uni.createFrom().item(Boolean.TRUE));

      // ACT / ASSERT
      ServiceResponseException ex =
          assertThrows(
              ServiceResponseException.class,
              () ->
                  groupItemService
                      .checkItemNotInGroup(context, 1, GroupItemType.FILE)
                      .await()
                      .indefinitely());

      assertEquals(ErrorCode.ITEM_ALREADY_IN_GROUP, ex.getErrorCode());
      assertEquals(
          "Item (1 - "
              + GroupItemType.FILE
              + ") is already in the group ("
              + context.getGroupId()
              + ").",
          ex.getMessage());
    }

    @Test
    void itemNotInGroup_ReturnsContext() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      when(groupItemsRepoMock.isItemInGroup(anyLong(), any(GroupItemType.class), anyLong()))
          .thenReturn(Uni.createFrom().item(Boolean.FALSE));

      // ACT / ASSERT
      assertDoesNotThrow(
          () ->
              groupItemService
                  .checkItemNotInGroup(context, 1, GroupItemType.FILE)
                  .await()
                  .indefinitely());
    }
  }

  @Nested
  class CheckItemInGroupTests {
    @Test
    void itemNotInGroup_ThrowsException() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      when(groupItemsRepoMock.isItemInGroup(anyLong(), any(GroupItemType.class), anyLong()))
          .thenReturn(Uni.createFrom().item(Boolean.FALSE));

      // ACT / ASSERT
      ServiceResponseException ex =
          assertThrows(
              ServiceResponseException.class,
              () ->
                  groupItemService
                      .checkItemInGroup(context, 1, GroupItemType.FILE)
                      .await()
                      .indefinitely());

      assertEquals(ErrorCode.ITEM_NOT_IN_GROUP, ex.getErrorCode());
      assertEquals(
          "Item (1 - "
              + GroupItemType.FILE
              + ") is not in the group ("
              + context.getGroupId()
              + ").",
          ex.getMessage());
    }

    @Test
    void itemInGroup_ReturnsContext() {
      // ARRANGE
      ServiceRequestContext context = ServiceRequestContextFaker.generateInstance();

      when(groupItemsRepoMock.isItemInGroup(anyLong(), any(GroupItemType.class), anyLong()))
          .thenReturn(Uni.createFrom().item(Boolean.TRUE));

      // ACT / ASSERT
      assertDoesNotThrow(
          () ->
              groupItemService
                  .checkItemInGroup(context, 1, GroupItemType.FILE)
                  .await()
                  .indefinitely());
    }
  }
}
