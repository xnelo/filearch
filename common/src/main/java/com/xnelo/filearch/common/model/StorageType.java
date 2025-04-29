package com.xnelo.filearch.common.model;

public enum StorageType {
  UNKOWN("UNK"),
  LOCAL_FILE_SYSTEM("LFS"),
  S3("S3S");

  private final String value;

  StorageType(String value) {
    this.value = value;
  }

  public String getDbValue() {
    return value;
  }

  public static StorageType fromString(String val) {
    return switch (val) {
      case "LFS" -> LOCAL_FILE_SYSTEM;
      case "S3S" -> S3;
      default -> UNKOWN;
    };
  }
}
