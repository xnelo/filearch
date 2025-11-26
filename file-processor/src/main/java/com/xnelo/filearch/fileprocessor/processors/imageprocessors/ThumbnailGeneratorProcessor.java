package com.xnelo.filearch.fileprocessor.processors.imageprocessors;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.service.storage.StorageService;
import com.xnelo.filearch.fileprocessorapi.contract.FileMetadata;
import com.xnelo.filearch.fileprocessorapi.processors.ImageProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ThumbnailGeneratorProcessor implements ImageProcessor {
  public static final int DEFAULT_THUMBNAIL_WIDTH = 100; // pixels
  public static final int DEFAULT_THUMBNAIL_HEIGHT = 100; // pixels

  @Inject StorageService storageService;

  @Override
  public int getOrder() {
    return 1000;
  }

  @Override
  public void processImage(
      FileMetadata metaData, BufferedImage originalImage, BufferedImage modifiedImage) {
    log.info("Creating thumbnail.");
    BufferedImage resizedImage =
        new BufferedImage(
            DEFAULT_THUMBNAIL_WIDTH, DEFAULT_THUMBNAIL_HEIGHT, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics2D = resizedImage.createGraphics();
    graphics2D.drawImage(
        originalImage, 0, 0, DEFAULT_THUMBNAIL_WIDTH, DEFAULT_THUMBNAIL_HEIGHT, null);
    graphics2D.dispose();
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {

      ImageIO.write(resizedImage, "png", os);

      ErrorCode ec = storageService.save(os.toByteArray(), metaData.storageKey() + ".thumb").await().indefinitely();
      log.info("saving file error code = {}", ec);
    } catch (Exception e) {
      log.error("Error creating thumbnail", e);
    }
  }
}
