package com.xnelo.filearch.restapi.api.mappers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class HttpStatusCodeMapperTest {
  @Nested
  public class RoundDownToNearest100Test {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10, 20, 99})
    public void numbersBelow100RoundTo0(int toTest) {
      Assertions.assertEquals(0, HttpStatusCodeMapper.roundDownToNearest100(toTest));
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 101, 105, 110, 120, 199})
    public void numbersBelow200RoundTo100(int toTest) {
      Assertions.assertEquals(100, HttpStatusCodeMapper.roundDownToNearest100(toTest));
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 205, 210, 220, 299})
    public void numbersBelow300RoundTo200(int toTest) {
      Assertions.assertEquals(200, HttpStatusCodeMapper.roundDownToNearest100(toTest));
    }

    @ParameterizedTest
    @ValueSource(ints = {300, 301, 305, 310, 320, 399})
    public void numbersBelow400RoundTo300(int toTest) {
      Assertions.assertEquals(300, HttpStatusCodeMapper.roundDownToNearest100(toTest));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 405, 410, 420, 499})
    public void numbersBelow500RoundTo400(int toTest) {
      Assertions.assertEquals(400, HttpStatusCodeMapper.roundDownToNearest100(toTest));
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 501, 505, 510, 520, 599})
    public void numbersBelow600RoundTo500(int toTest) {
      Assertions.assertEquals(500, HttpStatusCodeMapper.roundDownToNearest100(toTest));
    }
  }

  @Nested
  public class CombineStatusCodeTest {
    @Test
    public void newStatusCodeSet() {
      Assertions.assertEquals(422, HttpStatusCodeMapper.combineStatusCode(0, 422));
    }

    @Test
    public void lowerCodeSameClassSetToClassBase() {
      Assertions.assertEquals(400, HttpStatusCodeMapper.combineStatusCode(471, 422));
    }

    @Test
    public void higherCodeSameClassSetToClassBase() {
      Assertions.assertEquals(400, HttpStatusCodeMapper.combineStatusCode(401, 422));
    }

    @Test
    public void lowerCodeLowerClassSetToCurrent() {
      Assertions.assertEquals(422, HttpStatusCodeMapper.combineStatusCode(422, 398));
    }

    @Test
    public void higherCodeHigherClassSetToNew() {
      Assertions.assertEquals(501, HttpStatusCodeMapper.combineStatusCode(422, 501));
    }
  }
}
