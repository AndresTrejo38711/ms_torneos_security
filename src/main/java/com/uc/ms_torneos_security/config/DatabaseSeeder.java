package com.uc.ms_torneos_security.config;

import com.uc.ms_torneos_security.entity.AuthProvider;
import com.uc.ms_torneos_security.entity.HttpMethodType;
import com.uc.ms_torneos_security.entity.ModuleType;
import com.uc.ms_torneos_security.entity.Operation;
import com.uc.ms_torneos_security.entity.Permission;
import com.uc.ms_torneos_security.entity.PermissionEndpoint;
import com.uc.ms_torneos_security.entity.Role;
import com.uc.ms_torneos_security.entity.RolePermission;
import com.uc.ms_torneos_security.entity.User;
import com.uc.ms_torneos_security.entity.UserRole;
import com.uc.ms_torneos_security.repository.PermissionEndpointRepository;
import com.uc.ms_torneos_security.repository.PermissionRepository;
import com.uc.ms_torneos_security.repository.RolePermissionRepository;
import com.uc.ms_torneos_security.repository.RoleRepository;
import com.uc.ms_torneos_security.repository.UserRepository;
import com.uc.ms_torneos_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Al arrancar deja listos los permisos de este microservicio, el rol Administrador con todos
// ellos y el usuario administrador. Sin esto, con la API protegida nadie podría entrar.
// Solo crea lo que falta: si algo ya existe (por ejemplo, el administrador cambió un permiso), lo respeta.
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private static final String ADMIN_ROLE = "Administrador";

    // Un permiso por recurso + operación; cada uno con todos los endpoints que lo ejercen.
    private static final List<PermissionSeed> PERMISSIONS = List.of(
            permission("usuarios", Operation.VIEW, get("/api/users/{id}"), get("/api/users/{id}/detail")),
            permission("usuarios", Operation.LIST, get("/api/users")),
            permission("usuarios", Operation.CREATE, post("/api/users")),
            permission("usuarios", Operation.UPDATE, put("/api/users/{id}"),
                    patch("/api/users/{id}/disable"), patch("/api/users/{id}/enable")),
            permission("usuarios", Operation.DELETE, delete("/api/users/{id}")),

            permission("perfiles", Operation.VIEW, get("/api/users/{userId}/profile")),
            permission("perfiles", Operation.CREATE, post("/api/users/{userId}/profile")),
            permission("perfiles", Operation.UPDATE, put("/api/users/{userId}/profile")),
            permission("perfiles", Operation.DELETE, delete("/api/users/{userId}/profile")),

            permission("roles", Operation.VIEW, get("/api/roles/{id}")),
            permission("roles", Operation.LIST, get("/api/roles")),
            permission("roles", Operation.CREATE, post("/api/roles")),
            permission("roles", Operation.UPDATE, put("/api/roles/{id}")),
            permission("roles", Operation.DELETE, delete("/api/roles/{id}")),

            permission("permisos", Operation.VIEW, get("/api/permissions/{id}")),
            permission("permisos", Operation.LIST, get("/api/permissions")),
            permission("permisos", Operation.CREATE, post("/api/permissions")),
            permission("permisos", Operation.UPDATE, put("/api/permissions/{id}")),
            permission("permisos", Operation.DELETE, delete("/api/permissions/{id}")),

            permission("asignacion-roles", Operation.LIST, get("/api/user-roles"),
                    get("/api/user-roles/users/{userId}"), get("/api/user-roles/roles/{roleId}"),
                    get("/api/user-roles/roles/{roleId}/available-users")),
            permission("asignacion-roles", Operation.CREATE, post("/api/user-roles")),
            permission("asignacion-roles", Operation.DELETE, delete("/api/user-roles/{userId}/{roleId}")),

            permission("asignacion-permisos", Operation.VIEW, get("/api/role-permissions/roles/{roleId}/matrix"),
                    get("/api/role-permissions/users/{userId}/effective")),
            permission("asignacion-permisos", Operation.LIST, get("/api/role-permissions"),
                    get("/api/role-permissions/roles/{roleId}"),
                    get("/api/role-permissions/permissions/{permissionId}")),
            permission("asignacion-permisos", Operation.CREATE, post("/api/role-permissions")),
            permission("asignacion-permisos", Operation.UPDATE, put("/api/role-permissions/roles/{roleId}/matrix")),
            permission("asignacion-permisos", Operation.DELETE, delete("/api/role-permissions/{roleId}/{permissionId}"))
    );

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PermissionRepository permissionRepository;

    private final PermissionEndpointRepository permissionEndpointRepository;

    private final UserRoleRepository userRoleRepository;

    private final RolePermissionRepository rolePermissionRepository;

    private final PasswordEncoder passwordEncoder;

    @Value("${security.admin.email}")
    private String adminEmail;

    @Value("${security.admin.password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        Role adminRole = findOrCreateAdminRole();

        for (PermissionSeed seed : PERMISSIONS) {
            Permission permission = findOrCreatePermission(seed);
            if (!rolePermissionRepository.existsByRoleIdAndPermissionId(adminRole.getId(), permission.getId())) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRole(adminRole);
                rolePermission.setPermission(permission);
                rolePermissionRepository.save(rolePermission);
            }
        }

        User admin = findOrCreateAdmin();
        if (!userRoleRepository.existsByUserIdAndRoleId(admin.getId(), adminRole.getId())) {
            UserRole userRole = new UserRole();
            userRole.setUser(admin);
            userRole.setRole(adminRole);
            userRoleRepository.save(userRole);
        }

        log.info("Administrador listo: {} con el rol {} ({} permisos)", adminEmail, ADMIN_ROLE, PERMISSIONS.size());
    }

    private Role findOrCreateAdminRole() {
        return roleRepository.findByNameIgnoreCase(ADMIN_ROLE)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(ADMIN_ROLE);
                    role.setDescription("Acceso total al módulo de seguridad");
                    return roleRepository.save(role);
                });
    }

    private Permission findOrCreatePermission(PermissionSeed seed) {
        return permissionRepository.findByModelAndOperation(seed.model(), seed.operation())
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setModule(ModuleType.ADMINISTRACION);
                    permission.setModel(seed.model());
                    permission.setOperation(seed.operation());
                    for (EndpointSeed endpointSeed : seed.endpoints()) {
                        // Si alguien ya registró ese endpoint en otro permiso, no se duplica.
                        if (!permissionEndpointRepository.existsByUrlAndMethod(endpointSeed.url(), endpointSeed.method())) {
                            PermissionEndpoint endpoint = new PermissionEndpoint();
                            endpoint.setUrl(endpointSeed.url());
                            endpoint.setMethod(endpointSeed.method());
                            endpoint.setPermission(permission);
                            permission.getEndpoints().add(endpoint);
                        }
                    }
                    return permissionRepository.save(permission);
                });
    }

    // Si ese correo ya existe, se usa ese usuario tal como está (conserva su contraseña).
    private User findOrCreateAdmin() {
        return userRepository.findByEmail(adminEmail)
                .orElseGet(() -> {
                    User user = new User();
                    user.setName("Administrador");
                    user.setEmail(adminEmail);
                    user.setAuthProvider(AuthProvider.LOCAL);
                    user.setPassword(passwordEncoder.encode(adminPassword));
                    return userRepository.save(user);
                });
    }

    private static PermissionSeed permission(String model, Operation operation, EndpointSeed... endpoints) {
        return new PermissionSeed(model, operation, List.of(endpoints));
    }

    private static EndpointSeed get(String url) {
        return new EndpointSeed(url, HttpMethodType.GET);
    }

    private static EndpointSeed post(String url) {
        return new EndpointSeed(url, HttpMethodType.POST);
    }

    private static EndpointSeed put(String url) {
        return new EndpointSeed(url, HttpMethodType.PUT);
    }

    private static EndpointSeed patch(String url) {
        return new EndpointSeed(url, HttpMethodType.PATCH);
    }

    private static EndpointSeed delete(String url) {
        return new EndpointSeed(url, HttpMethodType.DELETE);
    }

    private record EndpointSeed(String url, HttpMethodType method) {
    }

    private record PermissionSeed(String model, Operation operation, List<EndpointSeed> endpoints) {
    }
}
