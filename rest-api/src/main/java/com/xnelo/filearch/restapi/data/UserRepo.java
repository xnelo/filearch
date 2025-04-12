package com.xnelo.filearch.restapi.data;

import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.common.model.mappers.UserMapper;
import com.xnelo.filearch.jooq.tables.Users;
import io.agroal.api.AgroalDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.mapstruct.factory.Mappers;

@RequestScoped
public class UserRepo {
  private final DSLContext context;
  private final UserMapper userMapper;

  @Inject
  public UserRepo(final AgroalDataSource dataSource) {
    this.context = DSL.using(dataSource, SQLDialect.POSTGRES);
    this.userMapper = Mappers.getMapper(UserMapper.class);
  }

  public Uni<User> createNewUser(final User newUser) {
    return Uni.createFrom()
        .item(
            context
                .insertInto(Users.USERS)
                .set(userMapper.toUsersRecord(newUser))
                .onConflictDoNothing()
                .returning()
                .fetchOne())
        .map(userMapper::toUserModel);
  }

  public Uni<User> getUserFromExternalId(final String externalId) {
    return Uni.createFrom()
        .item(
            context
                .selectFrom(Users.USERS)
                .where(Users.USERS.EXTERNAL_ID.eq(externalId))
                .fetchOne())
        .map(userMapper::toUserModel);
  }

  public Uni<Boolean> isUsernameUnique(final String username) {
    return Uni.createFrom()
        .item(
            context
                    .select(Users.USERS.USERNAME)
                    .from(Users.USERS)
                    .where(Users.USERS.USERNAME.equalIgnoreCase(username))
                    .fetchAny()
                == null);
  }
}
