package com.xnelo.filearch.restapi.api.contracts;

import com.xnelo.filearch.common.model.SortDirection;
import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PaginationRequest {
  @QueryParam("after")
  private Long after;

  @QueryParam("limit")
  private Integer limit;

  @QueryParam("direction")
  private SortDirection dir;
}
