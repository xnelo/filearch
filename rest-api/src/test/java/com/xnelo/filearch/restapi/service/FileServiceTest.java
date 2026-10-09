package com.xnelo.filearch.restapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.xnelo.filearch.common.data.ArtifactRepo;
import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.File;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.model.SortDirection;
import com.xnelo.filearch.common.service.PaginatedResponse;
import com.xnelo.filearch.common.service.ServiceActionResponse;
import com.xnelo.filearch.common.service.ServiceResponse;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.common.service.context.ServiceRequestContextImpl;
import com.xnelo.filearch.common.service.storage.StorageService;
import com.xnelo.filearch.common.testfixtures.fakers.FileFaker;
import com.xnelo.filearch.common.testfixtures.fakers.UserFaker;
import com.xnelo.filearch.common.testfixtures.fakers.UserTokenFaker;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.restapi.config.FilearchConfig;
import com.xnelo.filearch.restapi.data.FileTagsRepo;
import com.xnelo.filearch.restapi.data.GroupItemsRepo;
import com.xnelo.filearch.restapi.data.PaginatedData;
import com.xnelo.filearch.restapi.data.SequenceRepo;
import com.xnelo.filearch.restapi.data.StoredFilesRepo;
import com.xnelo.filearch.restapi.service.folder.FolderService;
import com.xnelo.filearch.restapi.service.tag.TagService;
import io.smallrye.mutiny.Uni;
import java.util.List;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class FileServiceTest {
  UserService mockUserService;
  SequenceRepo mockSequenceRepo;
  StorageService mockStorageService;
  StoredFilesRepo mockStoredFilesRepo;
  ArtifactRepo mockArtifactRepo;
  FolderService mockFolderService;
  FileTagsRepo mockFileTagsRepo;
  FilearchConfig mockConfig;
  TagService mockTagService;
  GroupItemsRepo mockGroupItemsRepo;
  GroupItemService mockGroupItemService;
  GroupService mockGroupService;
  GroupPermissionsService mockGroupPermissionsService;
  SharedTagsService mockSharedTagsService;
  Emitter<String> mockFileProcRequestEmitter;

  FileService fileService;

  @BeforeEach
  void setUp() {
    mockUserService = mock(UserService.class);
    mockSequenceRepo = mock(SequenceRepo.class);
    mockStorageService = mock(StorageService.class);
    mockStoredFilesRepo = mock(StoredFilesRepo.class);
    mockArtifactRepo = mock(ArtifactRepo.class);
    mockFolderService = mock(FolderService.class);
    mockFileTagsRepo = mock(FileTagsRepo.class);
    mockConfig = mock(FilearchConfig.class);
    mockTagService = mock(TagService.class);
    mockGroupItemsRepo = mock(GroupItemsRepo.class);
    mockGroupItemService = mock(GroupItemService.class);
    mockGroupService = mock(GroupService.class);
    mockGroupPermissionsService = mock(GroupPermissionsService.class);
    mockSharedTagsService = mock(SharedTagsService.class);
    mockFileProcRequestEmitter = mock(Emitter.class);

    fileService =
        new FileService(
            mockUserService,
            mockSequenceRepo,
            mockStorageService,
            mockStoredFilesRepo,
            mockArtifactRepo,
            mockFolderService,
            mockFileTagsRepo,
            mockConfig,
            mockTagService,
            mockGroupItemsRepo,
            mockGroupItemService,
            mockGroupService,
            mockGroupPermissionsService,
            mockSharedTagsService,
            mockFileProcRequestEmitter);
  }

  @Nested
  class GetFilesTagIsAssignedToTest {
    @Test
    void mapFunctionWorksCorrectly() {
      final long TAG_ID = 7L;

      File file1 = FileFaker.generateInstance();
      File file2 = FileFaker.generateInstance();
      UserToken userToken = UserTokenFaker.generateInstance();

      ServiceRequestContext requestContext =
          ServiceRequestContextImpl.builder()
              .resourceType(ResourceType.FILE)
              .actionType(ActionType.GET)
              .userToken(userToken)
              .user(UserFaker.from(userToken))
              .build();
      PaginationParameters paginationParameters =
          new PaginationParameters(null, 2, SortDirection.ASCENDING);

      when(mockUserService.checkUserExist(any(ServiceRequestContext.class)))
          .thenReturn(Uni.createFrom().item(requestContext));
      when(mockTagService.checkIfTagVisibleToUser(any(ServiceRequestContext.class), anyLong()))
          .thenReturn(Uni.createFrom().item(requestContext));
      when(mockStoredFilesRepo.getFilesTagAssignedTo(
              anyLong(), anyLong(), any(PaginationParameters.class)))
          .thenReturn(
              Uni.createFrom()
                  .item(
                      new PaginatedData<>(
                          null, List.of(file1, file2), paginationParameters.getDir(), false)));

      ServiceResponse<PaginatedResponse<File>> res =
          fileService
              .getFilesTagIsAssignedTo(requestContext, TAG_ID, paginationParameters)
              .await()
              .indefinitely();

      assertNotNull(res);
      assertEquals(1, res.getActionResponses().size());
      ServiceActionResponse<PaginatedResponse<File>> ar = res.getActionResponses().getFirst();
      assertEquals(ResourceType.FILE, ar.getResourceType());
      assertEquals(ActionType.GET, ar.getActionType());
      PaginatedResponse<File> pr = ar.getData();
      assertNotNull(pr);
      assertEquals(2, pr.getData().size());
      assertEquals(file1.getId(), pr.getData().getFirst().getId());
      assertEquals(file2.getId(), pr.getData().get(1).getId());
      assertFalse(pr.isHasNext());
    }
  }
}
