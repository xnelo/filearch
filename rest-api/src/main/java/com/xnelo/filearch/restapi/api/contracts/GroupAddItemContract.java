package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GroupAddItemContract(@JsonProperty("add_items") List<GroupItemContract> itemsToAdd) {}
