package com.dws.challenge.domain;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class TransferRequest {

    @NotNull
    @NotEmpty
    private final String fromAccountId;

    @NotNull
    @NotEmpty
    private final String toAccountId;

    @NotNull
    @DecimalMin(value = "0", inclusive = false, message = "Transfer amount must be greater than zero.")
    private BigDecimal amount;

    public TransferRequest(String fromAccountId, String toAccountId, BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }
}
