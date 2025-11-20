package com.xnelo.filearch.fileprocessor.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;

@Slf4j
@ApplicationScoped
public class FileProcessorHandler {
  @Incoming("file-proc-requests")
  public void consumeFileProcessRequestMessage(String data) {
    log.info("Received message: " + data);
  }
}
