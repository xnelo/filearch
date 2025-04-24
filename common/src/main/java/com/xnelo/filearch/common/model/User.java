package com.xnelo.filearch.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@AllArgsConstructor
@Getter
public class User {
  public static final ResourceType USER_RESOURCE_TYPE = ResourceType.USER;

  private long id;
  private String externalId;
  private String username;
  private String firstName;
  private String lastName;
  private String email;
}
