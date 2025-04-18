/*
 * This file is generated by jOOQ.
 */
package com.xnelo.filearch.jooq;

import org.jooq.Sequence;
import org.jooq.impl.DSL;
import org.jooq.impl.Internal;
import org.jooq.impl.SQLDataType;

/** Convenience access to all sequences in FILEARCH. */
@SuppressWarnings({"all", "unchecked", "rawtypes", "this-escape"})
public class Sequences {

  /** The sequence <code>FILEARCH.seq_file_upload_number</code> */
  public static final Sequence<Long> SEQ_FILE_UPLOAD_NUMBER =
      Internal.createSequence(
          "seq_file_upload_number",
          Filearch.FILEARCH,
          DSL.comment(""),
          SQLDataType.BIGINT.nullable(false),
          null,
          null,
          null,
          null,
          false,
          null);
}
