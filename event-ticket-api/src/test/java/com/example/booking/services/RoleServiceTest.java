package com.example.booking.services;


import com.example.booking.domain.entities.Role;
import com.example.booking.domain.enums.ERole;
import com.example.booking.exception.RoleNotFoundException;
import com.example.booking.exception.base.NotFoundException;
import com.example.booking.repositories.RoleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void findRoleEntityByName_ShouldReturnRole_WhenRoleExists() {

        ERole roleNameToFind = ERole.ROLE_USER;
        Role expectedRole = new Role(roleNameToFind);
        expectedRole.setId(1);

        when(roleRepository.findByName(roleNameToFind)).thenReturn(Optional.of(expectedRole));

        Role actualRole = roleService.findRoleEntityByName(roleNameToFind);

        assertNotNull(actualRole);
        assertEquals(expectedRole.getId(), actualRole.getId());
        assertEquals(expectedRole.getName(), actualRole.getName());

        verify(roleRepository, times(1)).findByName(roleNameToFind);
    }

    @Test
    void findRoleEntityByName_ShouldThrowRoleNotFoundException_WhenRoleDoesNotExist() {

        ERole roleNameToFind = ERole.ROLE_ADMIN;

        when(roleRepository.findByName(roleNameToFind)).thenReturn(Optional.empty());

        assertThrows(RoleNotFoundException.class, () -> {
            roleService.findRoleEntityByName(roleNameToFind);
        });

        verify(roleRepository, times(1)).findByName(roleNameToFind);
    }
}
