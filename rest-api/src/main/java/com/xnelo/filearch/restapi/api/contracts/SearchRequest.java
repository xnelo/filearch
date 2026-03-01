package com.xnelo.filearch.restapi.api.contracts;

import jakarta.ws.rs.QueryParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SearchRequest extends PaginationRequest {
  @QueryParam("search_term")
  private String searchTerm;
}
