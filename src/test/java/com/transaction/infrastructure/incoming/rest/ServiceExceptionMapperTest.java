package com.transaction.infrastructure.incoming.rest;

import com.transaction.domain.exception.Error;
import com.transaction.domain.exception.Errors;
import com.transaction.domain.exception.ServiceException;
import jakarta.json.JsonObject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceExceptionMapperTest {
    private final ServiceExceptionMapper mapper = new ServiceExceptionMapper();

    @Test
    void testNotFoundError() {
        ServiceException ex = new ServiceException(Errors.GetTransactionsErrors.NOT_FOUND);
        Response response = mapper.toResponse(ex);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        JsonObject json = (JsonObject) response.getEntity();
        assertEquals("0802", json.getString("errorCode"));
    }

    @Test
    void testInvalidInputError() {
        ServiceException ex = new ServiceException(Errors.CreateTransactionsErrors.INVALID_INPUT);
        Response response = mapper.toResponse(ex);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonObject json = (JsonObject) response.getEntity();
        assertEquals("01005", json.getString("errorCode"));
    }

    @Test
    void testPersistenceError() {
        ServiceException ex = new ServiceException(Errors.CreateTransactionsErrors.PERSISTENCE_ERROR);
        Response response = mapper.toResponse(ex);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        JsonObject json = (JsonObject) response.getEntity();
        assertEquals("01003", json.getString("errorCode"));
    }

    @Test
    void testLegacyErrorStillWorks() {
        ServiceException ex = new ServiceException(new Error("SOMETHING_ELSE"));
        Response response = mapper.toResponse(ex);
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        JsonObject json = (JsonObject) response.getEntity();
        assertEquals("SOMETHING_ELSE", json.getString("errorCode"));
    }
} 