package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xnelo.filearch.common.model.GroupItemType;
import com.xnelo.filearch.common.model.StorageType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GroupFileContract {
  @JsonProperty("id")
  private final long id;

  @JsonProperty("owner_id")
  private final Long ownerId;

  @JsonProperty("folder_id")
  private final Long folderId;

  @JsonProperty("storage_type")
  private final StorageType storageType;

  @JsonProperty("storage_key")
  private final String storageKey;

  @JsonProperty("original_filename")
  private final String originalFilename;

  @JsonProperty("mime_type")
  private final String mimeType;

  @JsonProperty("item_type")
  private final GroupItemType itemType;

  @JsonProperty("folder_in")
  private final String folderIn;

  @JsonProperty("folder_in_id")
  private final Long folderInId;
}
