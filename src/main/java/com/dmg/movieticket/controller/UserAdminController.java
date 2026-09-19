package com.dmg.movieticket.controller;

import com.dmg.movieticket.dto.request.UpdateUserRoleRequest;
import com.dmg.movieticket.dto.response.UserRoleResponse;
import com.dmg.movieticket.service.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @PatchMapping("/make-admin")
    public ResponseEntity<UserRoleResponse> makeAdmin(
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {

        return ResponseEntity.ok(
                userAdminService.makeAdmin(request)
        );
    }
}