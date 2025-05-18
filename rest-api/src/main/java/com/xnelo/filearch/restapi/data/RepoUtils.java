package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.common.model.SortDirection;
import org.jooq.*;

public class RepoUtils {
  public static SelectLimitPercentStep<?> addPagination(
      final SelectConditionStep<?> conditionStep,
      final Field<Long> fieldToSort,
      final Long after,
      final Integer limit,
      final SortDirection sortDirection) {
    SelectConditionStep<?> selectStatement = conditionStep;

    SortField<?> sortField;
    if (sortDirection == null || sortDirection == SortDirection.ASCENDING) {
      if (after != null && after >= 0) {
        selectStatement = selectStatement.and(fieldToSort.gt(after));
      }
      sortField = fieldToSort.asc();
    } else {
      if (after != null && after >= 0) {
        selectStatement = selectStatement.and(fieldToSort.lt(after));
      }
      sortField = fieldToSort.desc();
    }

    return selectStatement.orderBy(sortField).limit(limit != null && limit > 0 ? limit : 20);
  }
}
