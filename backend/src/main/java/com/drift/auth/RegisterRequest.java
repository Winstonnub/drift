// Defining JSON shapes for register
package com.drift.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

    @Email
    @NotBlank
    String email,

    @NotBlank
    @Size(min = 8, max = 100)
    String password,

    @NotBlank
    String displayName

) {
}