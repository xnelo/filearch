package com.xnelo.filearch.common.model;

import lombok.Getter;

@Getter
public class SearchParameters extends PaginationParameters {
  private final String searchTerm;

  public SearchParameters(
      String searchTerm, final Long after, final Integer limit, final SortDirection dir) {
    super(after, limit, dir);
    this.searchTerm = searchTerm;
  }
}
