/*
 * This file is generated by jOOQ.
 */
package com.xnelo.filearch.jooq.tables.pojos;

import java.io.Serializable;

/** This class is generated by jOOQ. */
@SuppressWarnings({"all", "unchecked", "rawtypes", "this-escape"})
public class StoredFiles implements Serializable {

  private static final long serialVersionUID = 1L;

  private final Integer id;
  private final Integer ownerUserId;
  private final String storageType;
  private final String storageKey;

  public StoredFiles(StoredFiles value) {
    this.id = value.id;
    this.ownerUserId = value.ownerUserId;
    this.storageType = value.storageType;
    this.storageKey = value.storageKey;
  }

  public StoredFiles(Integer id, Integer ownerUserId, String storageType, String storageKey) {
    this.id = id;
    this.ownerUserId = ownerUserId;
    this.storageType = storageType;
    this.storageKey = storageKey;
  }

  /** Getter for <code>FILEARCH.stored_files.id</code>. */
  public Integer getId() {
    return this.id;
  }

  /** Getter for <code>FILEARCH.stored_files.owner_user_id</code>. */
  public Integer getOwnerUserId() {
    return this.ownerUserId;
  }

  /** Getter for <code>FILEARCH.stored_files.storage_type</code>. */
  public String getStorageType() {
    return this.storageType;
  }

  /** Getter for <code>FILEARCH.stored_files.storage_key</code>. */
  public String getStorageKey() {
    return this.storageKey;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final StoredFiles other = (StoredFiles) obj;
    if (this.id == null) {
      if (other.id != null) return false;
    } else if (!this.id.equals(other.id)) return false;
    if (this.ownerUserId == null) {
      if (other.ownerUserId != null) return false;
    } else if (!this.ownerUserId.equals(other.ownerUserId)) return false;
    if (this.storageType == null) {
      if (other.storageType != null) return false;
    } else if (!this.storageType.equals(other.storageType)) return false;
    if (this.storageKey == null) {
      if (other.storageKey != null) return false;
    } else if (!this.storageKey.equals(other.storageKey)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
    result = prime * result + ((this.ownerUserId == null) ? 0 : this.ownerUserId.hashCode());
    result = prime * result + ((this.storageType == null) ? 0 : this.storageType.hashCode());
    result = prime * result + ((this.storageKey == null) ? 0 : this.storageKey.hashCode());
    return result;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("StoredFiles (");

    sb.append(id);
    sb.append(", ").append(ownerUserId);
    sb.append(", ").append(storageType);
    sb.append(", ").append(storageKey);

    sb.append(")");
    return sb.toString();
  }
}
