package com.xnelo.filearch.common.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ErrorCode {
  OK(0),
  NO_FIELDS_TO_UPDATE(50),
  NOT_IMPLEMENTED(99),

  // USER Service Errors 100 - 199
  USERNAME_MUST_BE_UNIQUE(100),
  USER_ALREADY_EXISTS(101),
  USER_DOES_NOT_EXIST(102),
  USERNAME_CANNOT_BE_UPDATED(103),
  USER_ID_CANNOT_BE_UPDATED(104),
  USER_ROOT_FOLDER_ID_CANNOT_BE_UPDATED(105),
  // END USER Service Errors

  // FILE Service Errors 200 - 299
  FILE_DOES_NOT_EXIST(200),
  // END FILE Service Errors

  // FOLDER Service Errors 300 - 399
  FOLDER_DOES_NOT_EXIST(300),
  FOLDER_WITH_NAME_ALREADY_EXISTS(301),
  FOLDER_ID_CANNOT_BE_UPDATED(302),
  FOLDER_OWNER_CANNOT_BE_UPDATED(303),
  // END FOLDER Service Errors

  // IO ERRORS 500 - 600
  UNABLE_TO_CREATE_DIR(500),
  UNABLE_TO_SAVE_FILE(501),
  UNABLE_TO_DELETE_FILE(502),
  UNABLE_TO_OPEN_INPUT_STREAM(503);
  // END IO ERRORS

  @JsonValue final int code;

  ErrorCode(int code) {
    this.code = code;
  }
}
