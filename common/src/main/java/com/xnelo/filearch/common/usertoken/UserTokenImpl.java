package com.xnelo.filearch.common.usertoken;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserTokenImpl implements UserToken {
  private final String id;
  private final String firstName;
  private final String lastName;
  private final String username;
  private final String email;
}
