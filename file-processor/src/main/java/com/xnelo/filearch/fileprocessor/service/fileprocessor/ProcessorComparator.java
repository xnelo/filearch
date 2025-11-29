package com.xnelo.filearch.fileprocessor.service.fileprocessor;

import com.xnelo.filearch.fileprocessorapi.processors.FileProcessor;
import java.util.Comparator;

public class ProcessorComparator implements Comparator<FileProcessor> {
  @Override
  public int compare(FileProcessor o1, FileProcessor o2) {
    if (o1.getOrder() == o2.getOrder()) {
      return o1.getClass().getName().compareTo(o2.getClass().getName());
    } else {
      return o1.getOrder() - o2.getOrder();
    }
  }
}
