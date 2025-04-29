package com.xnelo.filearch.common.model.mappers;

import com.xnelo.filearch.common.model.File;
import com.xnelo.filearch.common.model.StorageType;
import com.xnelo.filearch.jooq.tables.records.StoredFilesRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(imports = StorageType.class)
public interface FileMapper {
  @Mapping(target = "ownerId", source = "ownerUserId")
  @Mapping(
      target = "storageType",
      expression = "java( StorageType.fromString(toConvert.getStorageType()) )")
  File toFile(StoredFilesRecord toConvert);
}
