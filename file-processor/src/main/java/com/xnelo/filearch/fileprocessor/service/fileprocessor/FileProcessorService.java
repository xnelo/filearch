package com.xnelo.filearch.fileprocessor.service.fileprocessor;

import com.xnelo.filearch.common.messaging.ProcessFileRequest;
import java.util.Set;

public interface FileProcessorService {
  /**
   * A set of mime types that this processor can process.
   *
   * @return A set of mime types @see <a
   *     href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/MIME_types/Common_types">...</a>
   */
  Set<String> getMimeTypesProcessed();

  /**
   * the function that actually processes the file.
   *
   * @param request The request containing file information from the database.
   */
  void processFile(ProcessFileRequest request);
}
