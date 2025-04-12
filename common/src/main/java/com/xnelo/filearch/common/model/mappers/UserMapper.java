package com.xnelo.filearch.common.model.mappers;

import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.jooq.tables.records.UsersRecord;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {
  User toUserModel(final UsersRecord usersRecord);

  UsersRecord toUsersRecord(final User user);
}
