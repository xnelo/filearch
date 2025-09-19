package com.xnelo.filearch.common.service;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
// !! If any fields are added to this class; please double check
// !! the com.xnelo.filearch.restapi.api.mappers.PaginationMapper
// !! class to ensure all fields get mapped properly.
// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
@AllArgsConstructor
@Getter
public class PaginatedResponse<T> {
  private List<T> data;
  private boolean hasNext;
}
