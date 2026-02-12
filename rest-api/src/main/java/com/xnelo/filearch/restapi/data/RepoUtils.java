package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.model.SortDirection;
import java.util.List;
import org.jooq.*;

public class RepoUtils {
  public static final int DEFAULT_PAGINATION_LIMIT = 20;

  public static SelectLimitPercentStep<?> addPagination(
      final SelectConditionStep<?> conditionStep,
      final Field<Long> fieldToSort,
      final PaginationParameters paginationParameters) {
    SelectConditionStep<?> selectStatement = conditionStep;

    SortField<?> sortField;
    if (paginationParameters.getDir() == null
        || paginationParameters.getDir() == SortDirection.ASCENDING) {
      if (paginationParameters.getAfter() != null && paginationParameters.getAfter() >= 0) {
        selectStatement = selectStatement.and(fieldToSort.gt(paginationParameters.getAfter()));
      }
      sortField = fieldToSort.asc();
    } else {
      if (paginationParameters.getAfter() != null && paginationParameters.getAfter() >= 0) {
        selectStatement = selectStatement.and(fieldToSort.lt(paginationParameters.getAfter()));
      }
      sortField = fieldToSort.desc();
    }

    return selectStatement
        .orderBy(sortField)
        .limit(getLimitToUse(paginationParameters.getLimit()) + 1);
  }

  public static <T> PaginatedData<T> toPaginatedData(
      final List<T> toReturn, final PaginationParameters paginationParameters) {
    int limitToUse = getLimitToUse(paginationParameters.getLimit());
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
    return new PaginatedData<>(
        paginationParameters.getAfter(), finalToReturn, paginationParameters.getDir(), hasNext);
  }

  private static int getLimitToUse(final Integer passedInLimit) {
    if (passedInLimit != null && passedInLimit > 0) {
      return passedInLimit;
    } else {
      return DEFAULT_PAGINATION_LIMIT;
    }
  }
}
