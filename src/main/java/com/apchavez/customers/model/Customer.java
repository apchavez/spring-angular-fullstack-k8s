package com.apchavez.customers.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.annotation.Id;

public record Customer(

        @Id Integer id,

        @NotBlank(message = "The name is required") String name,

        @NotBlank(message = "The lastname is required") String lastname,

        @NotBlank(message = "The state is required") @Pattern(regexp = "^(active|inactive)$", message = "The state must be 'active' or 'inactive'") String state,

        @NotNull(message = "The age is required") @Min(value = 1, message = "The age must be greater than 0") Integer age) {
}