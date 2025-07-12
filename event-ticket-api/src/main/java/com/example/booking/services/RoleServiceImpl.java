package com.example.booking.services;

import com.example.booking.domain.entities.Role;
import com.example.booking.domain.enums.ERole;
import com.example.booking.exception.RoleNotFoundException;
import com.example.booking.repositories.RoleRepository;
import com.example.booking.services.intefaces.RoleService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role findRoleEntityByName(ERole name) {
        return roleRepository.findByName(name).orElseThrow(RoleNotFoundException::new);
    }
}
