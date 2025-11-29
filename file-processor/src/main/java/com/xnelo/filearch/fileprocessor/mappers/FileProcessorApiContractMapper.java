package com.xnelo.filearch.fileprocessor.mappers;

import com.xnelo.filearch.common.messaging.ProcessFileRequest;
import com.xnelo.filearch.fileprocessorapi.contract.FileMetadata;
import org.mapstruct.Mapper;

@Mapper
public interface FileProcessorApiContractMapper {
  FileMetadata toFileMetadata(ProcessFileRequest req);
}
