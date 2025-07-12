package com.example.booking.services;

import com.example.booking.domain.entities.Role;
import com.example.booking.domain.enums.ERole;
import com.example.booking.exception.RoleNotFoundException;
import com.example.booking.repositories.RoleRepository;
import com.example.booking.services.intefaces.RoleService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl implements RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleServiceImpl.class);

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public Role findRoleEntityByName(ERole name) {
        log.debug("Attempting to find role by name={}", name);
        return roleRepository.findByName(name)
                .orElseThrow(() -> {
                    log.warn("Role not found for name={}", name);
                    return new RoleNotFoundException();
                });
    }
}
