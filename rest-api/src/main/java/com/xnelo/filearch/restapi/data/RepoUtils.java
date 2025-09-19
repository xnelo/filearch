package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.common.model.SortDirection;
import java.util.List;
import org.jooq.*;

public class RepoUtils {
  public static final int DEFAULT_PAGINATION_LIMIT = 20;

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

    return selectStatement.orderBy(sortField).limit(getLimitToUse(limit) + 1);
  }

  public static <T> PaginatedData<T> toPaginatedData(
      final Long cursor,
      final List<T> toReturn,
      final SortDirection sortDirection,
      final Integer limit) {
    int limitToUse = getLimitToUse(limit);
    // when adding pagination to the query we retrieve one more record so we can determine if we
    // have more data after this call. So we need to return 1 less record.
    boolean hasNext = false;
    List<T> finalToReturn;
    if (toReturn.size() > limitToUse) {
      hasNext = true;
      finalToReturn = toReturn.subList(0, toReturn.size() - 1);
    } else {
      finalToReturn = toReturn;
    }
    return new PaginatedData<>(cursor, finalToReturn, sortDirection, hasNext);
  }

  private static int getLimitToUse(final Integer passedInLimit) {
    if (passedInLimit != null && passedInLimit > 0) {
      return passedInLimit;
    } else {
      return DEFAULT_PAGINATION_LIMIT;
    }
  }
}
