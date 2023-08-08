/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.apimgt.rest.api.publisher.v1.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.APIProvider;
import org.wso2.carbon.apimgt.api.model.GatewayPolicyData;
import org.wso2.carbon.apimgt.api.model.GatewayPolicyDeployment;
import org.wso2.carbon.apimgt.api.model.OperationPolicy;
import org.wso2.carbon.apimgt.api.model.OperationPolicyData;
import org.wso2.carbon.apimgt.rest.api.common.RestApiCommonUtil;
import org.wso2.carbon.apimgt.rest.api.common.RestApiConstants;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.GatewayPoliciesApiService;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.common.mappings.GatewayPolicyMappingUtil;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.common.mappings.OperationPolicyMappingUtil;

import org.apache.cxf.jaxrs.ext.MessageContext;

import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.GatewayPolicyDeploymentDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.GatewayPolicyMappingDataListDTO;
import org.wso2.carbon.apimgt.rest.api.publisher.v1.dto.GatewayPolicyMappingsDTO;
import org.wso2.carbon.apimgt.rest.api.util.utils.RestApiUtil;

import java.util.List;
import java.util.Map;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.Response;

import static org.wso2.carbon.apimgt.impl.APIConstants.UN_AUTHORIZED_ERROR_MESSAGE;

/**
 * This is the service implementation class for managing gateway policies.
 */
public class GatewayPoliciesApiServiceImpl implements GatewayPoliciesApiService {
    private static final Log log = LogFactory.getLog(GatewayPoliciesApiServiceImpl.class);

    /**
     * Add gateway policy mapping.
     *
     * @param gatewayPolicyMappingsDTO gateway policy mapping DTO
     * @param messageContext           message context
     * @return Response object containing gateway policy mapping DTO
     */
    public Response addGatewayPoliciesToFlows(GatewayPolicyMappingsDTO gatewayPolicyMappingsDTO,
            MessageContext messageContext) {

        if (gatewayPolicyMappingsDTO == null) {
            RestApiUtil.handleBadRequest("Gateway policy mapping list is empty", log);
        }
        try {
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            List<OperationPolicy> gatewayPolicyList = OperationPolicyMappingUtil.fromDTOToAPIOperationPoliciesList(
                    gatewayPolicyMappingsDTO.getPolicyMapping());
            String mappingDescription = gatewayPolicyMappingsDTO.getDescription();
            String displayName = gatewayPolicyMappingsDTO.getDisplayName();
            String policyMappingUUID = apiProvider.applyGatewayGlobalPolicies(gatewayPolicyList, mappingDescription,
                    displayName, tenantDomain);
            gatewayPolicyMappingsDTO.setId(policyMappingUUID);

            return Response.ok(gatewayPolicyMappingsDTO).build();
        } catch (APIManagementException e) {
            if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure("User is not authorized to apply policies ", e, log);
            } else {
                String errorMessage = "Error while adding gateway policy mapping ";
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        }
        return null;
    }

    /**
     * Deploy gateway policy to gateway environments.
     *
     * @param gatewayPolicyMappingId         gateway policy mapping id
     * @param gatewayPolicyDeploymentDTOList gateway policy deployment DTO list
     * @param messageContext                 message context
     * @return Response object containing gateway policy deployment DTO list
     */
    public Response engageGlobalPolicy(String gatewayPolicyMappingId,
            List<GatewayPolicyDeploymentDTO> gatewayPolicyDeploymentDTOList, MessageContext messageContext) {

        if (gatewayPolicyDeploymentDTOList == null) {
            RestApiUtil.handleBadRequest("Gateway policy deployment list is empty", log);
        }
        try {
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            Map<Boolean, List<GatewayPolicyDeployment>> gatewayPolicyDeploymentMap = GatewayPolicyMappingUtil.fromDTOToGatewayPolicyDeploymentMap(
                    gatewayPolicyMappingId, gatewayPolicyDeploymentDTOList);
            apiProvider.engageGatewayGlobalPolicies(gatewayPolicyDeploymentMap, tenantDomain, gatewayPolicyMappingId);
            for (GatewayPolicyDeploymentDTO gatewayPolicyDeploymentDTO : gatewayPolicyDeploymentDTOList) {
                gatewayPolicyDeploymentDTO.setMappingUUID(gatewayPolicyMappingId);
            }
            GenericEntity<List<GatewayPolicyDeploymentDTO>> entity = new GenericEntity<List<GatewayPolicyDeploymentDTO>>(
                    gatewayPolicyDeploymentDTOList) {
            };
            return Response.ok(entity).build();
        } catch (APIManagementException e) {
            if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure("User is not authorized to apply policies ", e, log);
            } else {
                String errorMessage = "Error while deploying gateway policy ";
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        }
        return null;
    }

    /**
     * Delete gateway policy mapping.
     *
     * @param gatewayPolicyMappingId gateway policy mapping id
     * @param messageContext         message context
     */
    public Response deleteGatewayPolicyByPolicyId(String gatewayPolicyMappingId, MessageContext messageContext) {

        if (StringUtils.isBlank(gatewayPolicyMappingId)) {
            RestApiUtil.handleBadRequest("Gateway policy mapping ID is empty", log);
        }
        try {
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            List<OperationPolicyData> operationPolicyDataList = apiProvider.getGatewayPolicyDataListByPolicyId(
                    gatewayPolicyMappingId, false);
            if (!operationPolicyDataList.isEmpty()) {
                apiProvider.deleteGatewayPolicyMappingByPolicyMappingId(gatewayPolicyMappingId, tenantDomain);
                return Response.ok().build();
            } else {
                RestApiUtil.handleResourceNotFoundError(
                        "Gateway policy mapping not found for the given Mapping ID : " +
                                gatewayPolicyMappingId, log);
            }
        } catch (APIManagementException e) {
            if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure("User is not authorized to delete policy mapping ",
                        e, log);
            } else {
                String errorMessage = "Error while deleting the gateway policy mapping ";
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        }
        return null;
    }

    /**
     * Retrieve all the gateway policy mappings.
     *
     * @param offset         starting index
     * @param limit          max number of objects returned
     * @param messageContext message context
     * @return Response object containing gateway policy mapping DTO list
     */
    @Override
    public Response getAllGatewayPolicies(Integer limit, Integer offset, MessageContext messageContext) {

        try {
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            offset = offset != null ? offset : RestApiConstants.PAGINATION_OFFSET_DEFAULT;
            List<GatewayPolicyData> gatewayPolicyData = apiProvider.getAllGatewayPolicyMappings(tenantDomain);
            // Set limit to the query param value or the count of all policy mappings
            limit = limit != null ? limit : gatewayPolicyData.size();
            GatewayPolicyMappingDataListDTO gatewayPolicyMappingDataListDTO =
                    GatewayPolicyMappingUtil.fromGatewayPolicyDataListToDTO(gatewayPolicyData, offset, limit);
            return Response.ok().entity(gatewayPolicyMappingDataListDTO).build();
        } catch (APIManagementException e) {
            if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure("User is not authorized to retrieve policy mappings ",
                        e, log);
            } else {
                String errorMessage = "Error while retrieving the gateway policy mappings ";
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        }
        return null;
    }

    /**
     * Retrieve gateway policy mapping content by policy mapping id.
     *
     * @param gatewayPolicyMappingId gateway policy mapping id
     * @param messageContext         message context
     * @return Response object containing gateway policy mapping DTO
     */
    @Override
    public Response getGatewayPolicyMappingContentByPolicyMappingId(String gatewayPolicyMappingId,
            MessageContext messageContext) {

        try {
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            GatewayPolicyData gatewayPolicyData = apiProvider.getGatewayPolicyMappingDataByPolicyMappingId(
                    gatewayPolicyMappingId, tenantDomain);
            GatewayPolicyMappingsDTO gatewayPolicyMappingsDTO = GatewayPolicyMappingUtil.fromGatewayPolicyDataToDTO(
                    gatewayPolicyData);
            return Response.ok().entity(gatewayPolicyMappingsDTO).build();
        } catch (APIManagementException e) {
            if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure("User is not authorized to retrieve policy mapping ",
                        e, log);
            } else {
                String errorMessage = "Error while retrieving the gateway policy mapping ";
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        }
        return null;
    }

    /**
     * Update gateway policy mapping.
     *
     * @param gatewayPolicyMappingId   gateway policy mapping id
     * @param gatewayPolicyMappingsDTO updated gateway policy mapping DTO
     * @param messageContext           message context
     * @return Response object containing processed gateway policy mapping DTO
     */
    @Override
    public Response updateGatewayPoliciesToFlows(String gatewayPolicyMappingId,
            GatewayPolicyMappingsDTO gatewayPolicyMappingsDTO, MessageContext messageContext) {

        if (gatewayPolicyMappingsDTO == null) {
            RestApiUtil.handleBadRequest("Gateway policy mapping list is empty", log);
        }
        try {
            String tenantDomain = RestApiCommonUtil.getLoggedInUserTenantDomain();
            APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
            List<OperationPolicy> gatewayPolicyList = OperationPolicyMappingUtil.fromDTOToAPIOperationPoliciesList(
                    gatewayPolicyMappingsDTO.getPolicyMapping());
            String mappingDescription = gatewayPolicyMappingsDTO.getDescription();
            String displayName = gatewayPolicyMappingsDTO.getDisplayName();
            String mappingID = apiProvider.updateGatewayGlobalPolicies(gatewayPolicyList, mappingDescription,
                    displayName, tenantDomain, gatewayPolicyMappingId);
            gatewayPolicyMappingsDTO.id(mappingID);
            return Response.ok(gatewayPolicyMappingsDTO).build();
        } catch (APIManagementException e) {
            if (isAuthorizationFailure(e)) {
                RestApiUtil.handleAuthorizationFailure("User is not authorized to apply policies", e, log);
            } else {
                String errorMessage = "Error while applying gateway policy";
                RestApiUtil.handleInternalServerError(errorMessage, e, log);
            }
        }
        return null;
    }

    private boolean isAuthorizationFailure(Exception e) {
        String errorMessage = e.getMessage();
        return errorMessage != null && errorMessage.contains(UN_AUTHORIZED_ERROR_MESSAGE);
    }
}
