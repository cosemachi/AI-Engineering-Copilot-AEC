package com.aec.api;

import com.aec.application.AecException;
import com.aec.application.AecNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<AecException> {

    @Override
    public Response toResponse(AecException exception) {
        Response.Status status = exception instanceof AecNotFoundException
                ? Response.Status.NOT_FOUND
                : Response.Status.BAD_REQUEST;
        return Response.status(status)
                .entity(Map.of("error", exception.getMessage()))
                .build();
    }
}
