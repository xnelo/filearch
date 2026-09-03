package com.xnelo.filearch.restapi.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "filearch")
public interface FilearchConfig {
  String dbHost();

  boolean encryptionEnabled();

  String encryptionKey();

  BulkActionsConfig bulkActions();
}
