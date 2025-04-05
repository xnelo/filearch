/*
 * This file is generated by jOOQ.
 */
package com.xnelo.filearch.jooq.tables;


import com.xnelo.filearch.jooq.Filearch;
import com.xnelo.filearch.jooq.Keys;
import com.xnelo.filearch.jooq.tables.records.StoredFilesRecord;

import java.util.Collection;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Name;
import org.jooq.PlainSQL;
import org.jooq.QueryPart;
import org.jooq.SQL;
import org.jooq.Schema;
import org.jooq.Select;
import org.jooq.Stringly;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes", "this-escape" })
public class StoredFiles extends TableImpl<StoredFilesRecord> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>FILEARCH.stored_files</code>
     */
    public static final StoredFiles STORED_FILES = new StoredFiles();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<StoredFilesRecord> getRecordType() {
        return StoredFilesRecord.class;
    }

    /**
     * The column <code>FILEARCH.stored_files.id</code>.
     */
    public final TableField<StoredFilesRecord, Integer> ID = createField(DSL.name("id"), SQLDataType.INTEGER.nullable(false), this, "");

    /**
     * The column <code>FILEARCH.stored_files.owner_user_id</code>.
     */
    public final TableField<StoredFilesRecord, Integer> OWNER_USER_ID = createField(DSL.name("owner_user_id"), SQLDataType.INTEGER, this, "");

    /**
     * The column <code>FILEARCH.stored_files.storage_type</code>.
     */
    public final TableField<StoredFilesRecord, String> STORAGE_TYPE = createField(DSL.name("storage_type"), SQLDataType.VARCHAR(3), this, "");

    /**
     * The column <code>FILEARCH.stored_files.storage_key</code>.
     */
    public final TableField<StoredFilesRecord, String> STORAGE_KEY = createField(DSL.name("storage_key"), SQLDataType.VARCHAR, this, "");

    private StoredFiles(Name alias, Table<StoredFilesRecord> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private StoredFiles(Name alias, Table<StoredFilesRecord> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>FILEARCH.stored_files</code> table reference
     */
    public StoredFiles(String alias) {
        this(DSL.name(alias), STORED_FILES);
    }

    /**
     * Create an aliased <code>FILEARCH.stored_files</code> table reference
     */
    public StoredFiles(Name alias) {
        this(alias, STORED_FILES);
    }

    /**
     * Create a <code>FILEARCH.stored_files</code> table reference
     */
    public StoredFiles() {
        this(DSL.name("stored_files"), null);
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : Filearch.FILEARCH;
    }

    @Override
    public UniqueKey<StoredFilesRecord> getPrimaryKey() {
        return Keys.STORED_FILES_PKEY;
    }

    @Override
    public StoredFiles as(String alias) {
        return new StoredFiles(DSL.name(alias), this);
    }

    @Override
    public StoredFiles as(Name alias) {
        return new StoredFiles(alias, this);
    }

    @Override
    public StoredFiles as(Table<?> alias) {
        return new StoredFiles(alias.getQualifiedName(), this);
    }

    /**
     * Rename this table
     */
    @Override
    public StoredFiles rename(String name) {
        return new StoredFiles(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public StoredFiles rename(Name name) {
        return new StoredFiles(name, null);
    }

    /**
     * Rename this table
     */
    @Override
    public StoredFiles rename(Table<?> name) {
        return new StoredFiles(name.getQualifiedName(), null);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public StoredFiles where(Condition condition) {
        return new StoredFiles(getQualifiedName(), aliased() ? this : null, null, condition);
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public StoredFiles where(Collection<? extends Condition> conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public StoredFiles where(Condition... conditions) {
        return where(DSL.and(conditions));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public StoredFiles where(Field<Boolean> condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public StoredFiles where(SQL condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public StoredFiles where(@Stringly.SQL String condition) {
        return where(DSL.condition(condition));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public StoredFiles where(@Stringly.SQL String condition, Object... binds) {
        return where(DSL.condition(condition, binds));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    @PlainSQL
    public StoredFiles where(@Stringly.SQL String condition, QueryPart... parts) {
        return where(DSL.condition(condition, parts));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public StoredFiles whereExists(Select<?> select) {
        return where(DSL.exists(select));
    }

    /**
     * Create an inline derived table from this table
     */
    @Override
    public StoredFiles whereNotExists(Select<?> select) {
        return where(DSL.notExists(select));
    }
}
