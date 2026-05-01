package com.xnelo.filearch.common.model;

/** Describes the types of items that are allowed to be shared in a group. */
public enum GroupItemType {
  UNKNOWN((short) 0),
  FOLDER((short) 1),
  FILE((short) 2);

  private final short value;

  GroupItemType(short value) {
    this.value = value;
  }

  public short getDbValue() {
    return value;
  }

  public static GroupItemType fromDbValue(short value) {
    return switch (value) {
      case 1 -> FOLDER;
      case 2 -> FILE;
      default -> GroupItemType.UNKNOWN;
    };
  }
}
