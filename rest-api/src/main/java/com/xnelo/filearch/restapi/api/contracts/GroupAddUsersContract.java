package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record GroupAddUsersContract(@JsonProperty("user_to_add") List<String> usersToAdd) {}
