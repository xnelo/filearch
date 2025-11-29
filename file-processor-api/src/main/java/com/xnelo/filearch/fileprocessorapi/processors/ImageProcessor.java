package com.xnelo.filearch.fileprocessorapi.processors;

import com.xnelo.filearch.fileprocessorapi.contract.FileMetadata;
import java.awt.image.BufferedImage;

public interface ImageProcessor extends FileProcessor {
  /**
   * Processing of an image occurs in this method. The modified image will be passed from processor
   * to processor. This means any modifications made by previous processors will be present in the
   * modifiedImage image. To maintain an original version of the image the originalImage image
   * SHOULD NEVER BE MODIFIED.
   *
   * @param metaData The image data stored in the Database.
   * @param originalImage The original image. THIS SHOULD NEVER BE MODIFIED.
   * @param modifiedImage The image where processing and modification is done.
   */
  void processImage(
      final FileMetadata metaData, final BufferedImage originalImage, BufferedImage modifiedImage);
}
