package com.dmg.movieticket.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequest(

        @NotBlank
        @Email
        String email

) {
}