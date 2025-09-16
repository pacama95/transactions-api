package com.transaction.infrastructure.incoming.rest.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;

@RegisterForReflection
@Schema(description = "Request to update market price for a position")
public record UpdateMarketDataRequest(
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "Price must be positive")
        @Schema(description = "New market price per share", example = "155.75", required = true)
        BigDecimal price
) {
}