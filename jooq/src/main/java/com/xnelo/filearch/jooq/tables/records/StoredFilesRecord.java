/*
 * This file is generated by jOOQ.
 */
package com.xnelo.filearch.jooq.tables.records;


import com.xnelo.filearch.jooq.tables.StoredFiles;

import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class StoredFilesRecord extends UpdatableRecordImpl<StoredFilesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>FILEARCH.stored_files.id</code>.
     */
    public StoredFilesRecord setId(Long value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>FILEARCH.stored_files.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>FILEARCH.stored_files.owner_user_id</code>.
     */
    public StoredFilesRecord setOwnerUserId(Long value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>FILEARCH.stored_files.owner_user_id</code>.
     */
    public Long getOwnerUserId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>FILEARCH.stored_files.folder_id</code>.
     */
    public StoredFilesRecord setFolderId(Long value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>FILEARCH.stored_files.folder_id</code>.
     */
    public Long getFolderId() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>FILEARCH.stored_files.storage_type</code>.
     */
    public StoredFilesRecord setStorageType(String value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>FILEARCH.stored_files.storage_type</code>.
     */
    public String getStorageType() {
        return (String) get(3);
    }

    /**
     * Setter for <code>FILEARCH.stored_files.storage_key</code>.
     */
    public StoredFilesRecord setStorageKey(String value) {
        set(4, value);
        return this;
    }

    /**
     * Getter for <code>FILEARCH.stored_files.storage_key</code>.
     */
    public String getStorageKey() {
        return (String) get(4);
    }

    /**
     * Setter for <code>FILEARCH.stored_files.original_filename</code>.
     */
    public StoredFilesRecord setOriginalFilename(byte[] value) {
        set(5, value);
        return this;
    }

    /**
     * Getter for <code>FILEARCH.stored_files.original_filename</code>.
     */
    public byte[] getOriginalFilename() {
        return (byte[]) get(5);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Long> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached StoredFilesRecord
     */
    public StoredFilesRecord() {
        super(StoredFiles.STORED_FILES);
    }

    /**
     * Create a detached, initialised StoredFilesRecord
     */
    public StoredFilesRecord(Long id, Long ownerUserId, Long folderId, String storageType, String storageKey, byte[] originalFilename) {
        super(StoredFiles.STORED_FILES);

        setId(id);
        setOwnerUserId(ownerUserId);
        setFolderId(folderId);
        setStorageType(storageType);
        setStorageKey(storageKey);
        setOriginalFilename(originalFilename);
        resetTouchedOnNotNull();
    }

    /**
     * Create a detached, initialised StoredFilesRecord
     */
    public StoredFilesRecord(com.xnelo.filearch.jooq.tables.pojos.StoredFiles value) {
        super(StoredFiles.STORED_FILES);

        if (value != null) {
            setId(value.getId());
            setOwnerUserId(value.getOwnerUserId());
            setFolderId(value.getFolderId());
            setStorageType(value.getStorageType());
            setStorageKey(value.getStorageKey());
            setOriginalFilename(value.getOriginalFilename());
            resetTouchedOnNotNull();
        }
    }
}
