package com.xnelo.filearch.restapi.api.contracts;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AssignTagContract(@JsonProperty("tag_id") long tagId) {}
