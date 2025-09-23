package com.transaction.infrastructure.incoming.rest;

import com.transaction.domain.exception.ServiceException;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ServiceExceptionMapper implements ExceptionMapper<ServiceException> {
    @Override
    public Response toResponse(ServiceException exception) {
        String errorCode = exception.error().code();
        Response.Status status = mapErrorToStatus(errorCode);
        JsonObject json = Json.createObjectBuilder()
                .add("errorCode", errorCode)
                .build();
        return Response.status(status)
                .entity(json)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response.Status mapErrorToStatus(String errorCode) {
        return switch (errorCode) {
            case "NOT_FOUND", "0602", "0702", "0802" ->
                    Response.Status.NOT_FOUND; // DeleteTransaction, UpdateTransaction, GetTransaction NOT_FOUND
            case "INVALID_INPUT", "0601", "0701", "0801", "01005" ->
                    Response.Status.BAD_REQUEST; // All INVALID_INPUT errors
            case "OPERATION_FAILED" -> Response.Status.INTERNAL_SERVER_ERROR;
            default -> Response.Status.INTERNAL_SERVER_ERROR;
        };
    }
} 