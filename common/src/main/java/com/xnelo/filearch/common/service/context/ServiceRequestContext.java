package com.xnelo.filearch.common.service.context;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.common.usertoken.UserToken;

public interface ServiceRequestContext {
  ResourceType getResourceType();

  ActionType getActionType();

  UserToken getUserToken();

  Long getGroupId();

  User getUser();

  void setUser(User newUser);

  void setData(String key, Object value);

  <T> T getDataAs(final String key, final Class<T> classType);

  boolean getBooleanData(String key, boolean defaultValue);

  long getLongData(String Key);
}
