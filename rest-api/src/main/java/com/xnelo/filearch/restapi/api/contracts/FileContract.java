package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xnelo.filearch.common.model.StorageType;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FileContract {
  @JsonProperty("id")
  private final Long id;

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
}
