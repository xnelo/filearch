package com.xnelo.filearch.common.testfixtures.fakers;

import com.github.javafaker.Faker;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.model.SortDirection;

public class PaginationParametersFaker {

  private static final Faker faker = new Faker();

  public static PaginationParameters generateInstance() {
    return new PaginationParameters(
        faker.random().nextLong(100),
        faker.random().nextInt(100),
        faker.options().option(SortDirection.class));
  }
}
