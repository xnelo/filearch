package com.xnelo.filearch.common.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ErrorCode {
  OK(0),
  NO_FIELDS_TO_UPDATE(50),

  // USER Service Errors 100 - 200
  USERNAME_MUST_BE_UNIQUE(100),
  USER_ALREADY_EXISTS(101),
  USER_DOES_NOT_EXIST(102),
  USERNAME_CANNOT_BE_UPDATED(103),
  USER_ID_CANNOT_BE_UPDATED(104),
  // END USER Service Errors

  // IO ERRORS 500 - 600
  UNABLE_TO_CREATE_DIR(500),
  UNABLE_TO_SAVE_FILE(501);
  // END IO ERRORS

  @JsonValue final int code;

  ErrorCode(int code) {
    this.code = code;
  }
}
