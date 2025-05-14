package com.xnelo.filearch.common.utils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Lists {
  public static <T> List<List<T>> partitionList(List<T> list, int partitionSize) {
    int numberOfPartitions = (int) Math.ceil((double) list.size() / partitionSize);
    return IntStream.range(0, numberOfPartitions)
        .mapToObj(
            i -> list.subList(i * partitionSize, Math.min((i + 1) * partitionSize, list.size())))
        .collect(Collectors.toList());
  }
}
