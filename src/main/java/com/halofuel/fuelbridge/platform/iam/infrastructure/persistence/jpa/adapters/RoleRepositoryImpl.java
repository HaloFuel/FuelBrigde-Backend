package com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.adapters;

import com.halofuel.fuelbridge.platform.iam.domain.model.entities.Role;
import com.halofuel.fuelbridge.platform.iam.domain.model.valueobjects.Roles;
import com.halofuel.fuelbridge.platform.iam.domain.repositories.RoleRepository;
import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.assemblers.RolePersistenceAssembler;
import com.halofuel.fuelbridge.platform.iam.infrastructure.persistence.jpa.repositories.RolePersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class RoleRepositoryImpl implements RoleRepository {

    private final RolePersistenceRepository roleRepository;

    public RoleRepositoryImpl(RolePersistenceRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Optional<Role> findByName(Roles name) {
        return roleRepository.findByName(name)
                .map(RolePersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<Role> findAll() {
        return roleRepository.findAll().stream()
                .map(RolePersistenceAssembler::toDomainFromPersistence)
                .toList();
    }

    @Override
    public Role save(Role role) {
        var entity = RolePersistenceAssembler.toPersistenceFromDomain(role);
        return RolePersistenceAssembler.toDomainFromPersistence(roleRepository.save(entity));
    }

    @Override
    public boolean existsByName(Roles name) {
        return roleRepository.existsByName(name);
    }
}
