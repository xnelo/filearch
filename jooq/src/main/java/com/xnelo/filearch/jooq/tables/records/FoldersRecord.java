/*
 * This file is generated by jOOQ.
 */
package com.xnelo.filearch.jooq.tables.records;


import com.xnelo.filearch.jooq.tables.Folders;

import org.jooq.Record1;
import org.jooq.impl.UpdatableRecordImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class FoldersRecord extends UpdatableRecordImpl<FoldersRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>FILEARCH.folders.id</code>.
     */
    public FoldersRecord setId(Long value) {
        set(0, value);
        return this;
    }

    /**
     * Getter for <code>FILEARCH.folders.id</code>.
     */
    public Long getId() {
        return (Long) get(0);
    }

    /**
     * Setter for <code>FILEARCH.folders.owner_user_id</code>.
     */
    public FoldersRecord setOwnerUserId(Long value) {
        set(1, value);
        return this;
    }

    /**
     * Getter for <code>FILEARCH.folders.owner_user_id</code>.
     */
    public Long getOwnerUserId() {
        return (Long) get(1);
    }

    /**
     * Setter for <code>FILEARCH.folders.parent_id</code>.
     */
    public FoldersRecord setParentId(Long value) {
        set(2, value);
        return this;
    }

    /**
     * Getter for <code>FILEARCH.folders.parent_id</code>.
     */
    public Long getParentId() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>FILEARCH.folders.name</code>.
     */
    public FoldersRecord setName(byte[] value) {
        set(3, value);
        return this;
    }

    /**
     * Getter for <code>FILEARCH.folders.name</code>.
     */
    public byte[] getName() {
        return (byte[]) get(3);
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
     * Create a detached FoldersRecord
     */
    public FoldersRecord() {
        super(Folders.FOLDERS);
    }

    /**
     * Create a detached, initialised FoldersRecord
     */
    public FoldersRecord(Long id, Long ownerUserId, Long parentId, byte[] name) {
        super(Folders.FOLDERS);

        setId(id);
        setOwnerUserId(ownerUserId);
        setParentId(parentId);
        setName(name);
        resetTouchedOnNotNull();
    }

    /**
     * Create a detached, initialised FoldersRecord
     */
    public FoldersRecord(com.xnelo.filearch.jooq.tables.pojos.Folders value) {
        super(Folders.FOLDERS);

        if (value != null) {
            setId(value.getId());
            setOwnerUserId(value.getOwnerUserId());
            setParentId(value.getParentId());
            setName(value.getName());
            resetTouchedOnNotNull();
        }
    }
}
