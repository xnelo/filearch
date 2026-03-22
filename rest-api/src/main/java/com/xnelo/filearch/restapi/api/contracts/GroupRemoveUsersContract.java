package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GroupRemoveUsersContract(
    @JsonProperty("user_to_remove") List<String> usersToRemove) {}
