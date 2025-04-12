package com.xnelo.filearch.common.model.mappers;

import com.xnelo.filearch.jooq.tables.records.UsersRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class UserMapperTest {
  UserMapper mapper;

  @BeforeEach
  public void setup() {
    mapper = Mappers.getMapper(UserMapper.class);
  }

  @Test
  public void testMapping() {
    UsersRecord source =
        new UsersRecord(10, "TESTUSERNAME", "TESTFIRST", "TESTLAST", "TESTEMAIL", "TEST USER ID");

    var target = mapper.toUserModel(source);

    Assertions.assertEquals(source.getId(), target.getId());
    Assertions.assertEquals(source.getUsername(), target.getUsername());
    Assertions.assertEquals(source.getFirstName(), target.getFirstName());
    Assertions.assertEquals(source.getLastName(), target.getLastName());
    Assertions.assertEquals(source.getEmail(), target.getEmail());
    Assertions.assertEquals(source.getExternalId(), target.getExternalId());
  }
}
