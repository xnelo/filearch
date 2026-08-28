package com.xnelo.filearch.restapi.service.tag;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xnelo.filearch.common.exception.ServiceResponseException;
import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.model.Tag;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.common.testfixtures.fakers.ServiceRequestContextFaker;
import com.xnelo.filearch.common.testfixtures.fakers.TagFaker;
import com.xnelo.filearch.restapi.data.FileTagsRepo;
import com.xnelo.filearch.restapi.data.SharedTagsRepo;
import com.xnelo.filearch.restapi.data.TagRepo;
import com.xnelo.filearch.restapi.service.FileService;
import com.xnelo.filearch.restapi.service.GroupItemService;
import com.xnelo.filearch.restapi.service.GroupService;
import com.xnelo.filearch.restapi.service.SharedTagsService;
import com.xnelo.filearch.restapi.service.UserService;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class TagServiceTest {

  private FileService fileServiceMock;
  private GroupService groupServiceMock;
  private GroupItemService groupItemServiceMock;
  private SharedTagsService sharedTagsServiceMock;
  private UserService userServiceMock;
  private FileTagsRepo fileTagsRepoMock;
  private SharedTagsRepo sharedTagsRepoMock;
  private TagRepo tagRepoMock;
  private TagService tagService;

  @BeforeEach
  void setUp() {
    fileServiceMock = mock(FileService.class);
    groupServiceMock = mock(GroupService.class);
    groupItemServiceMock = mock(GroupItemService.class);
    sharedTagsServiceMock = mock(SharedTagsService.class);
    userServiceMock = mock(UserService.class);
    fileTagsRepoMock = mock(FileTagsRepo.class);
    sharedTagsRepoMock = mock(SharedTagsRepo.class);
    tagRepoMock = mock(TagRepo.class);

    tagService =
        new TagService(
            fileServiceMock,
            groupServiceMock,
            groupItemServiceMock,
            sharedTagsServiceMock,
            userServiceMock,
            fileTagsRepoMock,
            sharedTagsRepoMock,
            tagRepoMock);
  }

  @AfterEach
  void cleanup() {
    tagService = null;

    tagRepoMock = null;
    sharedTagsRepoMock = null;
    fileTagsRepoMock = null;
    userServiceMock = null;
    sharedTagsServiceMock = null;
    groupItemServiceMock = null;
    groupServiceMock = null;
    fileServiceMock = null;
  }

  @Nested
  class CheckIfTagExistsTests {
    @Test
    void userNotInRequest_ThrowsException() {
      ServiceRequestContext serviceRequestContext =
          ServiceRequestContextFaker.generateInstanceNoUser();

      assertThrows(
          ServiceResponseException.class,
          () -> tagService.checkIfTagExists(serviceRequestContext, 1L).await().indefinitely());
    }

    @Test
    void tagNotExist_ThrowsException() {
      ServiceRequestContext serviceRequestContext = ServiceRequestContextFaker.generateInstance();

      when(tagRepoMock.getTagById(anyLong(), anyLong())).thenReturn(Uni.createFrom().nullItem());

      ServiceResponseException ex =
          assertThrows(
              ServiceResponseException.class,
              () -> tagService.checkIfTagExists(serviceRequestContext, 1L).await().indefinitely());

      assertEquals(ErrorCode.TAG_DOES_NOT_EXIST, ex.getErrorCode());
      assertEquals("Tag (1) does not exist.", ex.getMessage());
    }

    @Test
    void tagExist_success() {
      ServiceRequestContext serviceRequestContext = ServiceRequestContextFaker.generateInstance();

      Tag originalTagData = TagFaker.generateInstance();
      when(tagRepoMock.getTagById(anyLong(), anyLong()))
          .thenReturn(Uni.createFrom().item(originalTagData));

      assertDoesNotThrow(
          () -> tagService.checkIfTagExists(serviceRequestContext, 1L).await().indefinitely());

      Tag tagData = serviceRequestContext.getDataAs(TagService.TAG_DATA_KEY, Tag.class);
      assertNotNull(tagData);
      assertEquals(originalTagData.getId(), tagData.getId());
    }
  }
}
