package com.xnelo.filearch.common.encryption;

import static org.jooq.impl.DSL.field;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jooq.Field;

public class JooqFields {
  private static final boolean ENCRYPTION_ENABLED =
      ConfigProvider.getConfig().getValue("filearch.encryption-enabled", Boolean.class);

  public static Field<byte[]> encryptField(String toEncrypt, String key) {
    if (ENCRYPTION_ENABLED) {
      return field("pgp_sym_encrypt({0}, {1})", byte[].class, toEncrypt, key);
    } else {
      return field("convert_to({0}, 'UTF8')", byte[].class, toEncrypt);
    }
  }

  public static Field<String> decryptField(Field<byte[]> toDecrypt, String key) {
    if (ENCRYPTION_ENABLED) {
      return field("pgp_sym_decrypt_null_on_err({0}, {1}::text)", String.class, toDecrypt, key);
    } else {
      return field("convert_from({0}, 'UTF8')", String.class, toDecrypt);
    }
  }
}
