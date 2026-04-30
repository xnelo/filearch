package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GroupRemoveItemContract(
    @JsonProperty("remove_items") List<GroupItemContract> itemsToRemove) {}
