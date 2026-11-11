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

// Al arrancar deja listos los permisos de la plataforma, los roles iniciales y el usuario administrador.
// Sin esto, con la API protegida nadie podría entrar.
// Solo crea lo que falta: si algo ya existe (por ejemplo, el administrador cambió un permiso), lo respeta.
// Eso incluye los endpoints: si a un permiso le falta uno de los de esta lista (porque el microservicio
// agregó la ruta o porque alguien lo quitó), se le vuelve a agregar en el siguiente arranque, salvo que
// ese endpoint ya esté registrado en otro permiso. Así una ruta nueva queda protegida con solo sumarla
// aquí y reiniciar, sin tocar los roles que ya tienen el permiso.
//
// Los permisos de los otros microservicios también se siembran aquí, porque es aquí donde se guardan
// y se validan: ms_torneos_deportiva no tiene tablas de permisos, solo le pregunta a este servicio
// (POST /api/internal/security/permissions-validation) si el usuario puede llamar cada URL.
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private static final String ADMIN_ROLE = Role.ADMIN_NAME;

    // Rol de las HU del Anexo 2 ("Como administrador deportivo..."): solo el módulo GESTION_DEPORTIVA.
    private static final String SPORTS_ADMIN_ROLE = "Administrador deportivo";

    // Un permiso por recurso + operación; cada uno con todos los endpoints que lo ejercen.
    // Módulo ADMINISTRACION: este microservicio.
    private static final List<PermissionSeed> SECURITY_PERMISSIONS = List.of(
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

    // Módulo GESTION_DEPORTIVA: ms_torneos_deportiva (Anexo 2), con su prefijo /api/deportiva.
    private static final String SPORTS = "/api/deportiva";

    private static final List<PermissionSeed> SPORTS_PERMISSIONS = List.of(
            // HU-GD-01
            permission("deportes", Operation.VIEW, get(SPORTS + "/sports/{id}")),
            permission("deportes", Operation.LIST, get(SPORTS + "/sports")),
            permission("deportes", Operation.CREATE, post(SPORTS + "/sports")),
            permission("deportes", Operation.UPDATE, put(SPORTS + "/sports/{id}"),
                    patch(SPORTS + "/sports/{id}/disable"), patch(SPORTS + "/sports/{id}/enable")),
            permission("deportes", Operation.DELETE, delete(SPORTS + "/sports/{id}")),

            // HU-GD-02
            permission("equipos", Operation.VIEW, get(SPORTS + "/teams/{id}")),
            permission("equipos", Operation.LIST, get(SPORTS + "/teams")),
            permission("equipos", Operation.CREATE, post(SPORTS + "/teams")),
            permission("equipos", Operation.UPDATE, put(SPORTS + "/teams/{id}"),
                    patch(SPORTS + "/teams/{id}/disable"), patch(SPORTS + "/teams/{id}/enable")),
            permission("equipos", Operation.DELETE, delete(SPORTS + "/teams/{id}")),

            // HU-GD-03
            permission("asignacion-deportes", Operation.LIST, get(SPORTS + "/team-sports/teams/{teamId}"),
                    get(SPORTS + "/team-sports/sports/{sportId}")),
            permission("asignacion-deportes", Operation.CREATE, post(SPORTS + "/team-sports")),
            permission("asignacion-deportes", Operation.DELETE, delete(SPORTS + "/team-sports/{teamId}/{sportId}")),

            // HU-GD-04 (LIST incluye la búsqueda GET /players?search=, que usa el perfil deportivo)
            permission("jugadores", Operation.VIEW, get(SPORTS + "/players/{id}")),
            permission("jugadores", Operation.LIST, get(SPORTS + "/players")),
            permission("jugadores", Operation.CREATE, post(SPORTS + "/players")),
            permission("jugadores", Operation.UPDATE, put(SPORTS + "/players/{id}"),
                    patch(SPORTS + "/players/{id}/disable"), patch(SPORTS + "/players/{id}/enable")),
            permission("jugadores", Operation.DELETE, delete(SPORTS + "/players/{id}")),

            // HU-GD-05 (retirar cierra el contrato: es una actualización, no un borrado)
            permission("contratos", Operation.LIST, get(SPORTS + "/team-players/teams/{teamId}"),
                    get(SPORTS + "/team-players/players/{playerId}")),
            permission("contratos", Operation.CREATE, post(SPORTS + "/team-players")),
            permission("contratos", Operation.UPDATE, patch(SPORTS + "/team-players/{id}/retire")),

            // HU-GD-06
            permission("habilidades", Operation.VIEW, get(SPORTS + "/skills/{id}")),
            permission("habilidades", Operation.LIST, get(SPORTS + "/skills")),
            permission("habilidades", Operation.CREATE, post(SPORTS + "/skills")),
            permission("habilidades", Operation.UPDATE, put(SPORTS + "/skills/{id}"),
                    patch(SPORTS + "/skills/{id}/disable"), patch(SPORTS + "/skills/{id}/enable")),
            permission("habilidades", Operation.DELETE, delete(SPORTS + "/skills/{id}")),

            // HU-GD-07
            permission("asignacion-habilidades", Operation.LIST, get(SPORTS + "/player-skills/players/{playerId}"),
                    get(SPORTS + "/player-skills/skills/{skillId}")),
            permission("asignacion-habilidades", Operation.CREATE, post(SPORTS + "/player-skills")),
            permission("asignacion-habilidades", Operation.DELETE,
                    delete(SPORTS + "/player-skills/{playerId}/{skillId}")),

            // HU-GD-08
            permission("perfil-deportivo", Operation.VIEW, get(SPORTS + "/players/{id}/profile"))
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
        List<Permission> securityPermissions = findOrCreatePermissions(ModuleType.ADMINISTRACION, SECURITY_PERMISSIONS);
        List<Permission> sportsPermissions = findOrCreatePermissions(ModuleType.GESTION_DEPORTIVA, SPORTS_PERMISSIONS);

        // El Administrador tiene todos los permisos. Si le falta alguno, se le vuelve a dar en cada arranque.
        Role adminRole = roleRepository.findByNameIgnoreCase(ADMIN_ROLE)
                .orElseGet(() -> createRole(ADMIN_ROLE, "Acceso total a la plataforma"));
        grant(adminRole, securityPermissions);
        grant(adminRole, sportsPermissions);

        // El Administrador deportivo recibe sus permisos solo cuando se crea el rol: si después alguien
        // le quita o le agrega permisos desde la matriz, el seeder no se los vuelve a cambiar.
        if (!roleRepository.existsByNameIgnoreCase(SPORTS_ADMIN_ROLE)) {
            Role sportsAdminRole = createRole(SPORTS_ADMIN_ROLE,
                    "Gestiona deportes, equipos, jugadores, contratos y habilidades");
            grant(sportsAdminRole, sportsPermissions);
            log.info("Rol {} creado con {} permisos", SPORTS_ADMIN_ROLE, sportsPermissions.size());
        }

        User admin = findOrCreateAdmin();
        if (!userRoleRepository.existsByUserIdAndRoleId(admin.getId(), adminRole.getId())) {
            UserRole userRole = new UserRole();
            userRole.setUser(admin);
            userRole.setRole(adminRole);
            userRoleRepository.save(userRole);
        }

        log.info("Administrador listo: {} con el rol {} ({} permisos)", adminEmail, ADMIN_ROLE,
                securityPermissions.size() + sportsPermissions.size());
    }

    private Role createRole(String name, String description) {
        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        return roleRepository.save(role);
    }

    private List<Permission> findOrCreatePermissions(ModuleType module, List<PermissionSeed> seeds) {
        return seeds.stream().map(seed -> findOrCreatePermission(module, seed)).toList();
    }

    // Le da al rol los permisos que todavía no tenga.
    private void grant(Role role, List<Permission> permissions) {
        for (Permission permission : permissions) {
            if (!rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), permission.getId())) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRole(role);
                rolePermission.setPermission(permission);
                rolePermissionRepository.save(rolePermission);
            }
        }
    }

    private Permission findOrCreatePermission(ModuleType module, PermissionSeed seed) {
        Permission permission = permissionRepository.findByModelAndOperation(seed.model(), seed.operation())
                .orElseGet(() -> {
                    Permission created = new Permission();
                    created.setModule(module);
                    created.setModel(seed.model());
                    created.setOperation(seed.operation());
                    return created;
                });

        // Nuevo o ya existente, el permiso recibe los endpoints que le falten.
        int added = 0;
        for (EndpointSeed endpointSeed : seed.endpoints()) {
            // Si alguien ya registró ese endpoint (en este permiso o en otro), no se duplica.
            if (!permissionEndpointRepository.existsByUrlAndMethod(endpointSeed.url(), endpointSeed.method())) {
                PermissionEndpoint endpoint = new PermissionEndpoint();
                endpoint.setUrl(endpointSeed.url());
                endpoint.setMethod(endpointSeed.method());
                endpoint.setPermission(permission);
                permission.getEndpoints().add(endpoint);
                added++;
            }
        }

        if (permission.getId() != null && added > 0) {
            log.info("Permiso {} · {}: se le agregaron {} endpoints", seed.model(), seed.operation(), added);
        }
        return permissionRepository.save(permission);
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
