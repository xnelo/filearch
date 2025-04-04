package com.xnelo.filearch.common.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ErrorCode {
  OK(0),

  // IO ERRORS 500 - 600
  UNABLE_TO_CREATE_DIR(500),
  UNABLE_TO_SAVE_FILE(501);
  // END IO ERRORS

  @JsonValue final int code;

  ErrorCode(int code) {
    this.code = code;
  }
}
