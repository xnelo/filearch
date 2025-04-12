package com.xnelo.filearch.common.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.All;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.util.List;
import org.jboss.logging.Logger;

public class JsonUtil {
  private static final Logger LOG = Logger.getLogger(JsonUtil.class);
  private static ObjectMapper objectMapper;

  // Replaces the CDI producer for ObjectMapper built into Quarkus
  @Singleton
  @Produces
  ObjectMapper objectMapper(@All List<ObjectMapperCustomizer> customizers) {
    ObjectMapper mapper = getMapper();

    // Apply all ObjectMapperCustomizer beans (incl. Quarkus)
    for (ObjectMapperCustomizer customizer : customizers) {
      customizer.customize(mapper);
    }

    return mapper;
  }

  public static ObjectMapper getMapper() {
    if (objectMapper == null) {
      objectMapper = new ObjectMapper();
    }
    return objectMapper;
  }

  public static String toJsonString(Object toConvert) {
    try {
      return getMapper().writeValueAsString(toConvert);
    } catch (JsonProcessingException e) {
      LOG.errorf(
          e, "Error converting object(%s) to JSON.", toConvert.getClass().getCanonicalName());
      return toConvert.toString();
    }
  }
}
