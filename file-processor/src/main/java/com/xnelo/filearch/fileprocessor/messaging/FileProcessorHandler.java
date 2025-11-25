package com.xnelo.filearch.fileprocessor.messaging;

import com.xnelo.filearch.common.json.JsonUtil;
import com.xnelo.filearch.common.messaging.ProcessFileRequest;
import com.xnelo.filearch.common.service.storage.StorageService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

@Slf4j
@ApplicationScoped
public class FileProcessorHandler {
  @Inject StorageService storageService;

  @RunOnVirtualThread
  @Incoming("file-proc-requests")
  @Blocking("file-proc-workers")
  public CompletionStage<Void> consumeFileProcessRequestMessage(Message<String> msg) {
    String data = msg.getPayload();
    log.debug("Received message: message={}", data);
    ProcessFileRequest req = JsonUtil.toObject(data, ProcessFileRequest.class);
    if (req != null) {
      processFile(req);
      return msg.ack();
    } else {
      return msg.nack(
          new InvalidPropertiesFormatException(
              "The data was not a parsable JSON 'ProcessFileRequest' object."));
    }
  }

  void processFile(ProcessFileRequest req) {
    try (InputStream fileData = storageService.getFileData(req.storageKey()).await().indefinitely()) {

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
