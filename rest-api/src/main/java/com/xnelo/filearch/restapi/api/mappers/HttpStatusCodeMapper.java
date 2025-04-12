package com.xnelo.filearch.restapi.api.mappers;

public class HttpStatusCodeMapper {
  public static final int INITIAL_STATUS_CODE = 0;

  /**
   * Combine the new status with the following status resulting in a status that represents the most
   * serious error/error class. The following logic is applied; If the current status is 0 then the
   * new status is returned. If the new status class is greater than the current status class then
   * the new status is returned. If the status classes are the same then the status class is
   * returned. If the new status class is less than the current status class then the current status
   * is returned. NOTE: see <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">wiki
   * article</a> for an explanation of status classes.
   *
   * @param currentStatus The current status.
   * @param newStatus The new status code to combine.
   * @return a final status code combining the two.
   */
  public static int combineStatusCode(int currentStatus, int newStatus) {
    if (currentStatus <= INITIAL_STATUS_CODE) {
      return newStatus;
    }

    int newStatusClass = roundDownToNearest100(newStatus);
    if (newStatusClass > currentStatus) {
      return newStatus;
    }

    int currentStatusClass = roundDownToNearest100(currentStatus);
    if (currentStatusClass == newStatusClass) {
      return currentStatusClass;
    } else if (currentStatusClass > newStatusClass) {
      return currentStatus;
    } else {
      return newStatus;
    }
  }

  static int roundDownToNearest100(int number) {
    return (number / 100) * 100;
  }
}
