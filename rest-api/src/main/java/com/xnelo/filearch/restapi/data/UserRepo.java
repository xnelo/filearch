package com.xnelo.filearch.restapi.data;

import static com.xnelo.filearch.common.encryption.JooqFields.decryptField;
import static com.xnelo.filearch.common.encryption.JooqFields.encryptField;

import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.jooq.tables.Users;
import io.agroal.api.AgroalDataSource;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.DSL;

@RequestScoped
public class UserRepo {
  public static final String FIRST_NAME_COLUMN_NAME = Users.USERS.FIRST_NAME.getName();
  public static final String LAST_NAME_COLUMN_NAME = Users.USERS.LAST_NAME.getName();
  public static final String EMAIL_COLUMN_NAME = Users.USERS.EMAIL.getName();
  public static final String USERNAME_COLUMN_NAME = Users.USERS.USERNAME.getName();
  public static final String DECRYPTED_USERNAME_FIELD = "DECRYPT_USERNAME";
  public static final String DECRYPTED_EMAIL_FIELD = "DECRYPT_EMAIL";
  public static final String DECRYPTED_FIRSTNAME_FIELD = "DECRYPT_FIRSTNAME";
  public static final String DECRYPTED_LASTNAME_FIELD = "DECRYPT_LASTNAME";

  public static final List<String> FIELDS_TO_ENCRYPT =
      List.of(
          FIRST_NAME_COLUMN_NAME, LAST_NAME_COLUMN_NAME, EMAIL_COLUMN_NAME, USERNAME_COLUMN_NAME);

  private final DSLContext context;
  private final String encryptionKey;
  private final List<? extends SelectField<?>> allFields;

  @Inject
  public UserRepo(
      final AgroalDataSource dataSource,
      @ConfigProperty(name = "filearch.encryption-key", defaultValue = "LOCAL_DEV_ENCRYPTION_KEY")
          final String encryptionKey) {
    this.encryptionKey = encryptionKey;
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.context.setSchema("FILEARCH").execute();

    this.allFields =
        List.of(
            decryptField(Users.USERS.USERNAME, encryptionKey).as(DECRYPTED_USERNAME_FIELD),
            Users.USERS.EXTERNAL_ID,
            Users.USERS.ID,
            decryptField(Users.USERS.EMAIL, encryptionKey).as(DECRYPTED_EMAIL_FIELD),
            decryptField(Users.USERS.FIRST_NAME, encryptionKey).as(DECRYPTED_FIRSTNAME_FIELD),
            decryptField(Users.USERS.LAST_NAME, encryptionKey).as(DECRYPTED_LASTNAME_FIELD));
  }

  public Uni<User> createNewUser(final User newUser) {
    Map<String, Object> insertFields =
        Map.of(
            EMAIL_COLUMN_NAME,
            encryptField(newUser.getEmail(), encryptionKey),
            USERNAME_COLUMN_NAME,
            encryptField(newUser.getUsername(), encryptionKey),
            FIRST_NAME_COLUMN_NAME,
            encryptField(newUser.getFirstName(), encryptionKey),
            LAST_NAME_COLUMN_NAME,
            encryptField(newUser.getLastName(), encryptionKey),
            Users.USERS.EXTERNAL_ID.getName(),
            newUser.getExternalId());
    return Uni.createFrom()
        .item(
            context
                .insertInto(Users.USERS)
                .set(insertFields)
                .onConflictDoNothing()
                .returningResult(allFields)
                .fetchOne())
        .map(this::toUserModel);
  }

  public Uni<User> getUserFromExternalId(final String externalId) {
    return Uni.createFrom()
        .item(
            context
                .select(allFields)
                .from(Users.USERS)
                .where(Users.USERS.EXTERNAL_ID.eq(externalId))
                .fetchOne())
        .map(this::toUserModel);
  }

  public Uni<Boolean> isUsernameUnique(final String username) {
    return Uni.createFrom()
        .item(
            context
                    .select(decryptField(Users.USERS.USERNAME, encryptionKey))
                    .from(Users.USERS)
                    .where(
                        decryptField(Users.USERS.USERNAME, encryptionKey).equalIgnoreCase(username))
                    .fetchAny()
                == null);
  }

  public Uni<User> getUserFromId(final long userId) {
    return Uni.createFrom()
        .item(
            context.select(allFields).from(Users.USERS).where(Users.USERS.ID.eq(userId)).fetchOne())
        .map(this::toUserModel);
  }

  public Uni<User> updateUser(final long userId, Map<String, Object> updateFields) {
    Map<String, Object> encryptedUpdateFields = encryptFields(updateFields);
    return Uni.createFrom()
        .item(
            context
                .update(Users.USERS)
                .set(encryptedUpdateFields)
                .where(Users.USERS.ID.eq(userId))
                .returningResult(allFields)
                .fetchOne())
        .map(this::toUserModel);
  }

  Map<String, Object> encryptFields(Map<String, Object> fieldsMap) {
    Map<String, Object> returnMap = new HashMap<>(fieldsMap.size());

    fieldsMap.forEach(
        (key, value) -> {
          if (FIELDS_TO_ENCRYPT.contains(key)) {
            if (value instanceof String stringValue) {
              returnMap.put(key, encryptField(stringValue, encryptionKey));
            } else {
              Log.errorf(
                  "Error encrypting field. Value was not a string. field=%s type=%s",
                  key, value.getClass().getName());
              throw new RuntimeException("ERROR ENCRYPTING FIELD");
            }
          } else {
            returnMap.put(key, value);
          }
        });

    return returnMap;
  }

  User toUserModel(final Record toConvert) {
    if (toConvert == null) {
      return null;
    }

    return User.builder()
        .id(toConvert.get(Users.USERS.ID))
        .email(toConvert.get(DECRYPTED_EMAIL_FIELD, String.class))
        .firstName(toConvert.get(DECRYPTED_FIRSTNAME_FIELD, String.class))
        .lastName(toConvert.get(DECRYPTED_LASTNAME_FIELD, String.class))
        .username(toConvert.get(DECRYPTED_USERNAME_FIELD, String.class))
        .build();
  }
}
