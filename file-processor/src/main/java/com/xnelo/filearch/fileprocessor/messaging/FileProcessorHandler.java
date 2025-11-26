package com.xnelo.filearch.fileprocessor.messaging;

import com.xnelo.filearch.common.json.JsonUtil;
import com.xnelo.filearch.common.messaging.ProcessFileRequest;
import com.xnelo.filearch.fileprocessor.service.fileprocessor.FileProcessorService;
import io.quarkus.arc.All;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.reactive.messaging.annotations.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletionStage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;

@Slf4j
@ApplicationScoped
public class FileProcessorHandler {

  private final Map<String, List<FileProcessorService>> processors;

  @Inject
  public FileProcessorHandler(@All List<FileProcessorService> fileProcessors) {
    this.processors = new HashMap<>();
    for (FileProcessorService service : fileProcessors) {
      for (String mimeTypeProcessed : service.getMimeTypesProcessed()) {
        processors.computeIfAbsent(mimeTypeProcessed, k -> new ArrayList<>()).add(service);
      }
    }
  }

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
    if (req.mimeType() == null) {
      log.info("File request mime type is null, file not processed. fileId={}", req.fileId());
      return;
    }

    List<FileProcessorService> services = this.processors.get(req.mimeType());
    if (services == null) {
      log.error("No processors for meme type {}", req.mimeType());
      return;
    }

    for (FileProcessorService service : services) {
      try {
        service.processFile(req);
      } catch (Exception e) {
        log.error(
            "Error while processing file: fileId={} processor={}",
            req.fileId(),
            service.getClass().getName(),
            e);
      }
    }
  }
}
