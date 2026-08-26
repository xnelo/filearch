package com.xnelo.filearch.common.exception;

import com.xnelo.filearch.common.model.ErrorCode;
import lombok.Getter;

public class RepoException extends RuntimeException {
  @Getter private final ErrorCode errorCode;

  public RepoException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }
}
