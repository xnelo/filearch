package com.xnelo.filearch.common.model.mappers;

import com.xnelo.filearch.common.model.File;
import com.xnelo.filearch.common.model.StorageType;
import com.xnelo.filearch.jooq.tables.records.StoredFilesRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

public class FileMapperTest {
  @Test
  public void toFileTest() {
    final long FILE_ID = 12L;
    final long OWNER_ID = 6L;
    final String STORAGE_TYPE = "LFS";
    final String STORAGE_KEY = "TEST/KEY.png";
    FileMapper mapper = Mappers.getMapper(FileMapper.class);

    StoredFilesRecord input = new StoredFilesRecord(FILE_ID, OWNER_ID, STORAGE_TYPE, STORAGE_KEY);

    File res = mapper.toFile(input);

    Assertions.assertEquals(FILE_ID, res.getId());
    Assertions.assertEquals(OWNER_ID, res.getOwnerId());
    Assertions.assertEquals(StorageType.LOCAL_FILE_SYSTEM, res.getStorageType());
    Assertions.assertEquals(STORAGE_KEY, res.getStorageKey());
  }
}
