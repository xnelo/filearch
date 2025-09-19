package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.common.model.SortDirection;
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
public class PaginatedData<T> {
  private Long cursor;
  private List<T> data;
  private SortDirection sortDirection;
  private boolean hasNext;
}
