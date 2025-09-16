package com.transaction.infrastructure.incoming.rest;

import com.transaction.domain.exception.Error;
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
        Error error = exception.getError();
        Response.Status status = mapErrorToStatus(error);
        JsonObject json = Json.createObjectBuilder()
                .add("errorCode", error.code())
                .build();
        return Response.status(status)
                .entity(json)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    private Response.Status mapErrorToStatus(Error error) {
        return switch (error.code()) {
            case "NOT_FOUND" -> Response.Status.NOT_FOUND;
            case "INVALID_INPUT" -> Response.Status.BAD_REQUEST;
            case "OPERATION_FAILED" -> Response.Status.INTERNAL_SERVER_ERROR;
            default -> Response.Status.INTERNAL_SERVER_ERROR;
        };
    }
} 