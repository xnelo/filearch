package com.xnelo.filearch.fileprocessor.processors.imageprocessors;

import com.xnelo.filearch.common.model.ErrorCode;
import com.xnelo.filearch.common.service.storage.StorageService;
import com.xnelo.filearch.fileprocessorapi.contract.FileMetadata;
import com.xnelo.filearch.fileprocessorapi.processors.ImageProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class ThumbnailGeneratorProcessor implements ImageProcessor {
  public static final int DEFAULT_THUMBNAIL_WIDTH = 200; // pixels

  @Inject StorageService storageService;

  @Override
  public int getOrder() {
    return 1000;
  }

  @Override
  public void processImage(
      FileMetadata metaData, BufferedImage originalImage, BufferedImage modifiedImage) {
    log.info("Creating thumbnail. fileId={}", metaData.fileId());
    int newHeight =
        calculateHeight(
            originalImage.getWidth(), DEFAULT_THUMBNAIL_WIDTH, originalImage.getHeight());
    BufferedImage resizedImage =
        new BufferedImage(DEFAULT_THUMBNAIL_WIDTH, newHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics2D = resizedImage.createGraphics();
    graphics2D.drawImage(originalImage, 0, 0, DEFAULT_THUMBNAIL_WIDTH, newHeight, null);
    graphics2D.dispose();
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      ImageIO.write(resizedImage, "jpg", os);

      ErrorCode ec =
          storageService
              .save(os.toByteArray(), metaData.storageKey() + ".thumb.jpg")
              .await()
              .indefinitely();
      log.info("saving file error code = {}", ec);
    } catch (Exception e) {
      log.error("Error creating thumbnail", e);
    } finally {
      log.info("FINISHED creating thumbnail. fileId={}", metaData.fileId());
    }
  }

  private static int calculateHeight(int originalWidth, int newWidth, int originalHeight) {
    float ratio = (float) newWidth / (float) originalWidth;
    float newHeight = originalHeight * ratio;
    return Math.round(newHeight);
  }
}
