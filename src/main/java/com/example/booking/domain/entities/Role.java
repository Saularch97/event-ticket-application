package com.example.booking.domain.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "tb_roles")
public class Role {
    public Role() {
    }

    public Role(Long roleId, String name) {
        this.roleId = roleId;
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;
    private String name;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public enum Values {
        BASIC(1L),
        ADMIN(2L);

        final long roleId;

        Values(long roleId) {
            this.roleId = roleId;
        }

        public long getRoleId() {
            return roleId;
        }
    }
}
