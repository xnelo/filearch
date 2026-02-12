package com.xnelo.filearch.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaginationParameters {
  private Long after;
  private Integer limit;
  private SortDirection dir;
}
