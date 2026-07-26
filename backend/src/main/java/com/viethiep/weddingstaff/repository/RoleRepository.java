package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.Role;
import com.viethiep.weddingstaff.enumtype.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
