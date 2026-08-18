package com.xnelo.filearch.restapi.api.resources;

import com.xnelo.filearch.common.model.ActionType;
import com.xnelo.filearch.common.model.GroupMembershipStatus;
import com.xnelo.filearch.common.model.PaginationParameters;
import com.xnelo.filearch.common.model.ResourceType;
import com.xnelo.filearch.common.service.context.ServiceRequestContext;
import com.xnelo.filearch.common.service.context.ServiceRequestContextImpl;
import com.xnelo.filearch.common.usertoken.UserToken;
import com.xnelo.filearch.common.usertoken.UserTokenHandler;
import com.xnelo.filearch.restapi.api.contracts.AssignTagContract;
import com.xnelo.filearch.restapi.api.contracts.GroupAddItemContract;
import com.xnelo.filearch.restapi.api.contracts.GroupAddUsersContract;
import com.xnelo.filearch.restapi.api.contracts.GroupCreateContract;
import com.xnelo.filearch.restapi.api.contracts.GroupMemberPermissionModifyContract;
import com.xnelo.filearch.restapi.api.contracts.GroupRemoveItemContract;
import com.xnelo.filearch.restapi.api.contracts.GroupRemoveUsersContract;
import com.xnelo.filearch.restapi.api.contracts.PaginationRequest;
import com.xnelo.filearch.restapi.api.mappers.ContractMapper;
import com.xnelo.filearch.restapi.service.GroupItemService;
import com.xnelo.filearch.restapi.service.GroupPermissionsService;
import com.xnelo.filearch.restapi.service.GroupService;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.mapstruct.factory.Mappers;

@RequestScoped
@Path("group")
public class GroupResource {
  @Inject UserTokenHandler userTokenHandler;
  @Inject GroupService groupService;
  @Inject GroupPermissionsService groupPermissionsService;
  @Inject GroupItemService groupItemService;
  private final ContractMapper contractMapper = Mappers.getMapper(ContractMapper.class);

  @GET
  @RolesAllowed("user")
  public Uni<Response> getAll(@BeanParam PaginationRequest paginationRequest) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.GET)
            .userToken(userToken)
            .build();

    PaginationParameters paginationParameters =
        contractMapper.toPaginationParameters(paginationRequest);
    return groupService
        .getAllGroups(requestContext, paginationParameters)
        .map(
            paginatedServiceResponse ->
                contractMapper.toApiResponse(
                    paginatedServiceResponse,
                    resp ->
                        contractMapper.toPaginationContract(
                            resp, contractMapper::toGroupContractList)));
  }

  @POST
  @RolesAllowed("user")
  public Uni<Response> createNewGroup(final GroupCreateContract newGroup) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.CREATE)
            .userToken(userToken)
            .build();

    return groupService
        .createNewGroup(requestContext, newGroup)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toGroupContract));
  }

  @DELETE
  @RolesAllowed("user")
  @Path("{id}")
  public Uni<Response> deleteGroup(@PathParam("id") long groupId) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.DELETE)
            .userToken(userToken)
            .build();

    return groupService
        .deleteGroup(requestContext, groupId)
        .map(
            groupServiceResponse ->
                contractMapper.toApiResponse(
                    groupServiceResponse, contractMapper::toGroupContract));
  }

  @POST
  @RolesAllowed("user")
  @Path("{id}/add_users")
  public Uni<Response> addUser(@PathParam("id") long groupId, GroupAddUsersContract usersToAdd) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.ADD_USER_TO_GROUP)
            .userToken(userToken)
            .build();

    return groupService
        .addUsersToGroup(requestContext, groupId, usersToAdd)
        .map(
            serviceResponse -> contractMapper.toApiResponse(serviceResponse, username -> username));
  }

  @POST
  @RolesAllowed("user")
  @Path("{id}/remove_users")
  public Uni<Response> removeUser(
      @PathParam("id") long groupId, GroupRemoveUsersContract usersToRemove) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.REMOVE_USER_FROM_GROUP)
            .userToken(userToken)
            .build();

    return groupService
        .removeUsersFromGroup(requestContext, groupId, usersToRemove)
        .map(
            serviceResponse -> contractMapper.toApiResponse(serviceResponse, username -> username));
  }

  @POST
  @RolesAllowed("user")
  @Path("{id}/accept_invite")
  public Uni<Response> acceptInvite(@PathParam("id") long groupId) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.ACCEPT_GROUP_INVITE)
            .userToken(userToken)
            .build();

    return groupService
        .acceptGroupInvitation(requestContext, groupId)
        .map(serviceResponse -> contractMapper.toApiResponse(serviceResponse, success -> success));
  }

  @GET
  @RolesAllowed("user")
  @Path("groups_in")
  public Uni<Response> getGroupsIn(
      @BeanParam PaginationRequest paginationRequest,
      @QueryParam("membership_status") GroupMembershipStatus membershipStatus) {
    PaginationParameters paginationParameters =
        contractMapper.toPaginationParameters(paginationRequest);
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.GET)
            .userToken(userToken)
            .build();

    return groupService
        .getGroupsIn(requestContext, membershipStatus, paginationParameters)
        .map(
            paginatedServiceResponse ->
                contractMapper.toApiResponse(
                    paginatedServiceResponse,
                    resp ->
                        contractMapper.toPaginationContract(
                            resp, contractMapper::toGroupContractList)));
  }

  @POST
  @RolesAllowed("user")
  @Path("{id}/add_items")
  public Uni<Response> addItems(@PathParam("id") long groupId, GroupAddItemContract itemsToAdd) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.ADD_ITEM_TO_GROUP)
            .userToken(userToken)
            .build();

    return groupService
        .addItemsToGroup(requestContext, groupId, itemsToAdd)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toGroupItemContract));
  }

  @POST
  @RolesAllowed("user")
  @Path("{id}/remove_items")
  public Uni<Response> removeItems(
      @PathParam("id") long groupId, GroupRemoveItemContract itemsToRemove) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.REMOVE_ITEM_FROM_GROUP)
            .userToken(userToken)
            .build();

    return groupService
        .removeItemsFromGroup(requestContext, groupId, itemsToRemove)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(serviceResponse, contractMapper::toGroupItemContract));
  }

  @GET
  @RolesAllowed("user")
  @Path("{id}/files")
  public Uni<Response> getFilesInGroup(
      @PathParam("id") long groupId, @BeanParam PaginationRequest paginationRequest) {
    PaginationParameters paginationParameters =
        contractMapper.toPaginationParameters(paginationRequest);
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.GET)
            .userToken(userToken)
            .build();

    return groupService
        .getFilesInGroup(requestContext, groupId, paginationParameters)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(
                    serviceResponse,
                    resp ->
                        contractMapper.toPaginationContract(
                            resp, contractMapper::toGroupFileContractList)));
  }

  @POST
  @RolesAllowed("user")
  @Path("{id}/{file_id}/tag_file")
  public Uni<Response> tagFile(
      @PathParam("id") long groupId,
      @PathParam("file_id") long fileId,
      final AssignTagContract assignTag) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.TAG)
            .actionType(ActionType.ASSIGN)
            .userToken(userToken)
            .build();

    return groupItemService
        .assignTagToGroupFile(requestContext, groupId, fileId, assignTag.tagId())
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(
                    serviceResponse, (Boolean isSuccessful) -> isSuccessful));
  }

  @POST
  @RolesAllowed("user")
  @Path("{id}/{file_id}/untag_file")
  public Uni<Response> untagFile(
      @PathParam("id") long groupId,
      @PathParam("file_id") long fileId,
      final AssignTagContract unassignTag) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.TAG)
            .actionType(ActionType.UNASSIGN)
            .userToken(userToken)
            .build();

    return groupItemService
        .unassignTagFromGroupFile(requestContext, groupId, fileId, unassignTag.tagId())
        .map(
            booleanServiceResponse ->
                contractMapper.toApiResponse(
                    booleanServiceResponse, (Boolean isSuccessful) -> isSuccessful));
  }

  @GET
  @RolesAllowed("user")
  @Path("{id}/permissions")
  public Uni<Response> getPermissions(
      @PathParam("id") long groupId, @QueryParam("user_id") long userId) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.GET_GROUP_PERMISSIONS)
            .userToken(userToken)
            .build();

    return groupPermissionsService
        .getUserPermissions(requestContext, userId, groupId)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(
                    serviceResponse, contractMapper::toGroupMemberPermissionContractList));
  }

  @POST
  @RolesAllowed("user")
  @Path("{id}/permissions")
  public Uni<Response> modifyPermissions(
      @PathParam("id") long groupId, List<GroupMemberPermissionModifyContract> permissions) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.MODIFY_GROUP_PERMISSIONS)
            .userToken(userToken)
            .build();

    return groupPermissionsService
        .modifyPermissions(requestContext, groupId, permissions)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(
                    serviceResponse, contractMapper::toGroupMemberPermissionContract));
  }

  @GET
  @RolesAllowed("user")
  @Path("{id}/users_in_group")
  public Uni<Response> getUsersInGroup(@PathParam("id") long groupId) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.GET_USERS_IN_GROUP)
            .userToken(userToken)
            .build();

    return groupService
        .getUsersInGroup(requestContext, groupId)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(
                    serviceResponse, contractMapper::toGroupMemberContractList));
  }

  @GET
  @RolesAllowed("user")
  @Path("{id}/all_user_permissions")
  public Uni<Response> getAllUserPermissionsInGroup(@PathParam("id") long groupId) {
    UserToken userToken = userTokenHandler.getUserInfo();

    ServiceRequestContext requestContext =
        ServiceRequestContextImpl.builder()
            .resourceType(ResourceType.GROUP)
            .actionType(ActionType.GET_GROUP_PERMISSIONS)
            .userToken(userToken)
            .build();

    return groupPermissionsService
        .getAllGroupPermissionByUser(requestContext, groupId)
        .map(
            serviceResponse ->
                contractMapper.toApiResponse(
                    serviceResponse, contractMapper::toGroupMemberAllPermissionsContractList));
  }
}
