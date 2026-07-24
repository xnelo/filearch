package com.xnelo.filearch.restapi.service.tag;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class TagShareResult {
  private final Long tagId;
  private final Long groupId;
  private final boolean actionSuccessful;
}
