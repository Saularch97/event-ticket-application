package com.example.booking.services;


import com.example.booking.domain.entities.Role;
import com.example.booking.domain.enums.ERole;
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
    void findRoleEntityByName_ShouldThrowEntityNotFoundException_WhenRoleDoesNotExist() {

        ERole roleNameToFind = ERole.ROLE_ADMIN;

        when(roleRepository.findByName(roleNameToFind)).thenReturn(Optional.empty());

        EntityNotFoundException thrownException = assertThrows(EntityNotFoundException.class, () -> {
            roleService.findRoleEntityByName(roleNameToFind);
        });

        assertEquals("Role not found!", thrownException.getMessage());

        verify(roleRepository, times(1)).findByName(roleNameToFind);
    }
}
