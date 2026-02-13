package com.xnelo.filearch.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaginationParameters {
  private final Long after;
  private final Integer limit;
  private final SortDirection dir;
}
