package com.xnelo.filearch.fileprocessor.service.fileprocessor;

import com.xnelo.filearch.fileprocessorapi.processors.FileProcessor;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProcessorComparatorTest {
  @Test
  public void testOrderCorrect() {
    List<FileProcessor> processorList = new ArrayList<>();
    processorList.add(new FP2b());
    processorList.add(new FP1());
    processorList.add(new FP2c());
    processorList.add(new FP2a());
    processorList.add(new FP3());

    processorList.sort(new ProcessorComparator());

    Assertions.assertEquals("FP1", processorList.get(0).getClass().getSimpleName());
    Assertions.assertEquals("FP2a", processorList.get(1).getClass().getSimpleName());
    Assertions.assertEquals("FP2b", processorList.get(2).getClass().getSimpleName());
    Assertions.assertEquals("FP2c", processorList.get(3).getClass().getSimpleName());
    Assertions.assertEquals("FP3", processorList.get(4).getClass().getSimpleName());
  }

  private static class FP1 implements FileProcessor {
    @Override
    public int getOrder() {
      return 1;
    }
  }

  private static class FP2a implements FileProcessor {
    @Override
    public int getOrder() {
      return 2;
    }
  }

  private static class FP2b implements FileProcessor {
    @Override
    public int getOrder() {
      return 2;
    }
  }

  private static class FP2c implements FileProcessor {
    @Override
    public int getOrder() {
      return 2;
    }
  }

  private static class FP3 implements FileProcessor {
    @Override
    public int getOrder() {
      return 3;
    }
  }
}
