package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserContract {
  @JsonProperty("id")
  private final Integer id;

  @JsonProperty("first_name")
  private final String firstName;

  @JsonProperty("last_name")
  private final String lastName;

  @JsonProperty("username")
  private final String username;

  @JsonProperty("email")
  private final String email;
}
