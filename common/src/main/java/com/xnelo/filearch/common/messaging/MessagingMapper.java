package com.xnelo.filearch.common.messaging;

import com.xnelo.filearch.common.model.File;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface MessagingMapper {
  @Mapping(target = "fileId", source = "id")
  ProcessFileRequest toFileRequest(File file);
}
