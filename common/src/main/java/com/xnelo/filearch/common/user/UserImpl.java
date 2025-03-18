package com.xnelo.filearch.common.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserImpl implements User {
  private final String id;
}
