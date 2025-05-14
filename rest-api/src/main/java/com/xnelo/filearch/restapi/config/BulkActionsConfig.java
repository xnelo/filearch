package com.xnelo.filearch.restapi.config;

import io.smallrye.config.WithDefault;

public interface BulkActionsConfig {
  @WithDefault("100")
  int maxDelete();
}
