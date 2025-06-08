package com.example.booking.services.intefaces;

import com.example.booking.domain.entities.Role;
import com.example.booking.domain.enums.ERole;

public interface RoleService {
    Role findRoleEntityByName(ERole name);
}
