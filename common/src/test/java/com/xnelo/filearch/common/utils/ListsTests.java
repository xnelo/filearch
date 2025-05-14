package com.xnelo.filearch.common.utils;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ListsTests {
  @Nested
  public class PartitionListTests {
    @Test
    public void listPartitionedCorrectlyListSize10PartitionSize3() {
      var toPartition = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      var partitioned = Lists.partitionList(toPartition, 3);
      Assertions.assertEquals(4, partitioned.size());
      Assertions.assertEquals(List.of(1, 2, 3), partitioned.getFirst());
      Assertions.assertEquals(List.of(4, 5, 6), partitioned.get(1));
      Assertions.assertEquals(List.of(7, 8, 9), partitioned.get(2));
      Assertions.assertEquals(List.of(10), partitioned.get(3));
    }

    @Test
    public void listPartitionedCorrectlyListSize10PartitionSize4() {
      var toPartition = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      var partitioned = Lists.partitionList(toPartition, 4);
      Assertions.assertEquals(3, partitioned.size());
      Assertions.assertEquals(List.of(1, 2, 3, 4), partitioned.getFirst());
      Assertions.assertEquals(List.of(5, 6, 7, 8), partitioned.get(1));
      Assertions.assertEquals(List.of(9, 10), partitioned.get(2));
    }

    @Test
    public void listPartitionedCorrectlyListSize10PartitionSize5() {
      var toPartition = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
      var partitioned = Lists.partitionList(toPartition, 5);
      Assertions.assertEquals(2, partitioned.size());
      Assertions.assertEquals(List.of(1, 2, 3, 4, 5), partitioned.getFirst());
      Assertions.assertEquals(List.of(6, 7, 8, 9, 10), partitioned.get(1));
    }
  }
}
