package com.xnelo.filearch.common.encryption;

import static org.jooq.impl.DSL.field;

import org.jooq.Field;

public class JooqFields {
  public static Field<byte[]> encryptField(String toEncrypt, String key) {
    return field("pgp_sym_encrypt({0}, {1})", byte[].class, toEncrypt, key);
  }

  public static Field<String> decryptField(Field<byte[]> toDecrypt, String key) {
    return field("pgp_sym_decrypt_null_on_err({0}, {1}::text)", String.class, toDecrypt, key);
  }
}
