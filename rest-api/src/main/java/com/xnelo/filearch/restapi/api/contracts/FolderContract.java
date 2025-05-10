package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FolderContract {
  @JsonProperty("id")
  private final Long id;

  @JsonProperty("owner_user_id")
  private final Long ownerId;

  @JsonProperty("parent_id")
  private final Long parentId;

  @JsonProperty("folder_name")
  private final String folderName;
}
