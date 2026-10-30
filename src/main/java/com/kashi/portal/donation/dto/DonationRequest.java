package com.kashi.portal.donation.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DonationRequest {

    @NotBlank
    @Size(max = 100)
    private String donorName;

    @NotBlank
    @Email
    @Size(max = 150)
    private String donorEmail;

    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$")
    private String donorPhone;

    @NotNull
    @DecimalMin("1.00")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal amount;
}