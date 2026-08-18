package com.xnelo.filearch.common.service.context;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.model.User;
import com.xnelo.filearch.common.usertoken.UserToken;
import java.util.HashMap;
import java.util.Map;

public class ServiceRequestContextImpl implements ServiceRequestContext {
  private final ResourceType resourceType;
  private final ActionType actionType;
  private final UserToken userToken;
  private final Long groupId;

  private User user;

  private final Map<String, Object> data;

  private ServiceRequestContextImpl(
      final ResourceType resourceType,
      final ActionType actionType,
      final UserToken userToken,
      final Long groupId,
      final User user,
      final Map<String, Object> data) {
    this.resourceType = resourceType;
    this.actionType = actionType;
    this.userToken = userToken;
    this.groupId = groupId;
    this.user = user;
    this.data = data;
  }

  public static ServiceRequestContextImplBuilder builder() {
    return new ServiceRequestContextImplBuilder();
  }

  @Override
  public ResourceType getResourceType() {
    return resourceType;
  }

  @Override
  public ActionType getActionType() {
    return actionType;
  }

  @Override
  public UserToken getUserToken() {
    return userToken;
  }

  @Override
  public Long getGroupId() {
    return groupId;
  }

  @Override
  public User getUser() {
    return user;
  }

  @Override
  public void setUser(User newUser) {
    this.user = newUser;
  }

  @Override
  public void setData(String key, Object value) {
    if (key == null) {
      throw new IllegalArgumentException("key cannot be null");
    }
    this.data.put(key, value);
  }

  @Override
  public <T> T getDataAs(final String key, final Class<T> classType) {
    if (classType == null) {
      throw new IllegalArgumentException("classType cannot be null");
    }

    Object rawData = this.data.get(key);

    if (rawData == null) {
      return null;
    } else if (classType.isInstance(rawData)) {
      return classType.cast(rawData);
    } else {
      // TODO: Create a custom exception
      throw new RuntimeException("Data is not instance of '" + classType.getCanonicalName() + "'.");
    }
  }

  @Override
  public boolean getBooleanData(String key, boolean defaultValue) {
    Object value = this.data.get(key);
    if (value == null) {
      return defaultValue;
    } else if (value instanceof Boolean) {
      return (Boolean) value;
    } else {
      return defaultValue;
    }
  }

  @Override
  public long getLongData(String Key) {
    // TODO: Change thrown errors to a custom class
    Object value = this.data.get(Key);
    if (value == null) {
      throw new RuntimeException("Data was null. It cannot be null in this method");
    } else if (value instanceof Long) {
      return (Long) value;
    } else {
      throw new RuntimeException("Data was not a Long datatype.");
    }
  }

  public static class ServiceRequestContextImplBuilder {
    private ResourceType resourceType;
    private ActionType actionType;
    private UserToken userToken;
    private Long groupId;

    private User user;

    private final Map<String, Object> data = new HashMap<>();

    public ServiceRequestContextImplBuilder resourceType(ResourceType resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    public ServiceRequestContextImplBuilder actionType(ActionType actionType) {
      this.actionType = actionType;
      return this;
    }

    public ServiceRequestContextImplBuilder userToken(UserToken userToken) {
      this.userToken = userToken;
      return this;
    }

    public ServiceRequestContextImplBuilder groupId(Long groupId) {
      this.groupId = groupId;
      return this;
    }

    public ServiceRequestContextImplBuilder user(User user) {
      this.user = user;
      return this;
    }

    public ServiceRequestContextImplBuilder addData(String key, Object value) {
      if (key == null) {
        throw new IllegalArgumentException("key cannot be null");
      }
      this.data.put(key, value);
      return this;
    }

    public ServiceRequestContextImpl build() {
      return new ServiceRequestContextImpl(
          resourceType, actionType, userToken, groupId, user, data);
    }
  }
}
