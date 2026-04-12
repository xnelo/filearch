package com.xnelo.filearch.common.model;

/** Describes the permissions that may be granted to a user who is a member of a group. */
public enum GroupPermissionType {
  UNKNOWN((short) 0),
  ADMIN((short) 1),
  ADD_MEMBERS((short) 2),
  REMOVE_MEMBERS((short) 3),
  EDIT_MEMBER_PERMISSIONS((short) 4),
  ADD_ITEMS((short) 5),
  REMOVE_ITEMS((short) 6),
  TAG_ITEMS((short) 7),
  REMOVE_TAGS((short) 8);

  private final short value;

  GroupPermissionType(short value) {
    this.value = value;
  }

  public short getDbValue() {
    return value;
  }

  public static GroupPermissionType fromDbValue(short value) {
    return switch (value) {
      case 1 -> ADMIN;
      case 2 -> ADD_MEMBERS;
      case 3 -> REMOVE_MEMBERS;
      case 4 -> EDIT_MEMBER_PERMISSIONS;
      case 5 -> ADD_ITEMS;
      case 6 -> REMOVE_ITEMS;
      case 7 -> TAG_ITEMS;
      case 8 -> REMOVE_TAGS;
      default -> GroupPermissionType.UNKNOWN;
    };
  }
}
