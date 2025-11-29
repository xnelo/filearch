package com.xnelo.filearch.fileprocessorapi.processors;

public interface FileProcessor {
  /**
   * This value defines the order in which file processors are executed on the file being processed.
   *
   * @return An integer value used to order the processors.
   */
  int getOrder();
}
