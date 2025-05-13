package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FileBulkDeleteContract(@JsonProperty("files_to_delete") List<Long> fileIdsToDelete) {}
