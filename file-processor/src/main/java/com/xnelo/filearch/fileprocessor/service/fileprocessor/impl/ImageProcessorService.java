package com.xnelo.filearch.fileprocessor.service.fileprocessor.impl;

import com.xnelo.filearch.common.messaging.ProcessFileRequest;
import com.xnelo.filearch.common.service.storage.StorageService;
import com.xnelo.filearch.fileprocessor.mappers.FileProcessorApiContractMapper;
import com.xnelo.filearch.fileprocessor.service.fileprocessor.FileProcessorService;
import com.xnelo.filearch.fileprocessor.service.fileprocessor.ProcessorComparator;
import com.xnelo.filearch.fileprocessorapi.contract.FileMetadata;
import com.xnelo.filearch.fileprocessorapi.processors.ImageProcessor;
import io.quarkus.arc.All;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;

@Slf4j
@ApplicationScoped
public class ImageProcessorService implements FileProcessorService {
  private final Set<String> MEME_TYPES_PROCESSED = Set.of("image/png", "image/jpeg");
  private final List<ImageProcessor> imageProcessors;
  private final StorageService storageService;
  private final FileProcessorApiContractMapper contractMapper =
      Mappers.getMapper(FileProcessorApiContractMapper.class);

  @Inject
  public ImageProcessorService(
      @All List<ImageProcessor> imageProcessorList, final StorageService storageService) {
    this.storageService = storageService;
    this.imageProcessors = new ArrayList<>(imageProcessorList);
    this.imageProcessors.sort(new ProcessorComparator());
  }

  @Override
  public Set<String> getMimeTypesProcessed() {
    return MEME_TYPES_PROCESSED;
  }

  @Override
  public void processFile(ProcessFileRequest request) {
    BufferedImage originalImage;
    try (InputStream fileData =
        storageService.getFileData(request.storageKey()).await().indefinitely()) {
      originalImage = ImageIO.read(fileData);
    } catch (IOException e) {
      log.error("Error loading image into BufferedImage object.", e);
      originalImage = null;
    }

    if (originalImage == null) {
      log.info("Image was null.");
      return;
    }

    FileMetadata metadata = contractMapper.toFileMetadata(request);
    BufferedImage processedImage = makeDeepCopy(originalImage);

    for (ImageProcessor processor : this.imageProcessors) {
      try {
        processor.processImage(metadata, originalImage, processedImage);
      } catch (Exception e) {
        log.error(
            "Error while processing an image. fileId={} imageProcessorName={}",
            metadata.fileId(),
            processor.getClass().getName(),
            e);
      }
    }
  }

  static BufferedImage makeDeepCopy(BufferedImage toCopy) {
    return new BufferedImage(
        toCopy.getColorModel(),
        toCopy.copyData(null),
        toCopy.getColorModel().isAlphaPremultiplied(),
        null);
  }
}
