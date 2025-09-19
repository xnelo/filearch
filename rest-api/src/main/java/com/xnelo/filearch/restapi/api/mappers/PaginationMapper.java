package com.xnelo.filearch.restapi.api.mappers;

import com.xnelo.filearch.common.service.PaginatedResponse;
import com.xnelo.filearch.restapi.data.PaginatedData;
import org.mapstruct.Mapper;

@Mapper
public interface PaginationMapper {
  default <T> PaginatedResponse<T> toPaginatedResponse(PaginatedData<T> toConvert) {
    return new PaginatedResponse<>(toConvert.getData(), toConvert.isHasNext());
  }
}
