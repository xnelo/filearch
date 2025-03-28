package com.xnelo.filearch.common.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum ErrorCode {
  OK(0);

  @JsonValue final int code;

  ErrorCode(int code) {
    this.code = code;
  }
}
