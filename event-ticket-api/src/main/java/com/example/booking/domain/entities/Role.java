package com.example.booking.domain.entities;


import com.example.booking.dto.RoleItemDto;
import com.example.booking.domain.enums.ERole;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "tb_roles")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERole name;

    public Role() {

    }

    public Role(ERole name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ERole getName() {
        return name;
    }

    public void setName(ERole name) {
        this.name = name;
    }

    public static RoleItemDto toRoleItemDto(Role role) {
        return new RoleItemDto(
                role.getId(),
                role.getName().name()
        );
    }
}