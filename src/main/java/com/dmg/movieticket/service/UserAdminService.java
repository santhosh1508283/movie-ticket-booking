package com.dmg.movieticket.service;

import com.dmg.movieticket.dto.request.UpdateUserRoleRequest;
import com.dmg.movieticket.dto.response.UserRoleResponse;

public interface UserAdminService {

    UserRoleResponse makeAdmin(
            UpdateUserRoleRequest request
    );
}