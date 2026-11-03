package com.uc.ms_torneos_security.service;

import com.uc.ms_torneos_security.entity.ModuleType;
import com.uc.ms_torneos_security.entity.Role;
import com.uc.ms_torneos_security.entity.RolePermission;
import com.uc.ms_torneos_security.entity.User;
import com.uc.ms_torneos_security.entity.UserRole;
import com.uc.ms_torneos_security.exception.ApplicationException;
import com.uc.ms_torneos_security.exception.ErrorCase;
import com.uc.ms_torneos_security.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;

// Reglas para que la plataforma nunca se quede sin quien la administre: siempre debe quedar al menos
// un usuario activo con el rol Administrador, y ese rol conserva los permisos del módulo Administración.
// Sin ellas, un administrador podía desactivarse, borrarse o quitarse el rol (o quitarle al rol los
// permisos de seguridad) y nadie podría volver a gestionar usuarios, roles ni permisos.
@Component
@RequiredArgsConstructor
public class AdminGuard {

    private final UserRoleRepository userRoleRepository;

    public void checkCanDisable(User target, Long currentUserId) {
        if (target.getId().equals(currentUserId)) {
            throw invalid("No puedes desactivar tu propia cuenta: pídeselo a otro administrador");
        }
        if (isLastActiveAdmin(target)) {
            throw invalid("No se puede desactivar al último administrador activo: primero asigna el rol "
                    + Role.ADMIN_NAME + " a otro usuario");
        }
    }

    public void checkCanDelete(User target, Long currentUserId) {
        if (target.getId().equals(currentUserId)) {
            throw invalid("No puedes eliminar tu propia cuenta: pídeselo a otro administrador");
        }
        if (isLastActiveAdmin(target)) {
            throw invalid("No se puede eliminar al último administrador activo: primero asigna el rol "
                    + Role.ADMIN_NAME + " a otro usuario");
        }
    }

    // Retirar cualquier otro rol (incluso uno propio) se permite: solo se cuida el de Administrador.
    public void checkCanRemoveRole(UserRole userRole, Long currentUserId) {
        if (!isAdminRole(userRole.getRole())) {
            return;
        }
        if (userRole.getUser().getId().equals(currentUserId)) {
            throw invalid("No puedes quitarte a ti mismo el rol " + Role.ADMIN_NAME
                    + ": pídeselo a otro administrador");
        }
        if (isLastActiveAdmin(userRole.getUser())) {
            throw invalid("No se puede quitar el rol " + Role.ADMIN_NAME + " al último administrador activo");
        }
    }

    // Las reglas dependen del nombre del rol: si se renombrara, dejarían de protegerlo.
    public void checkCanRename(Role role, String newName) {
        if (isAdminRole(role) && !Role.ADMIN_NAME.equalsIgnoreCase(newName)) {
            throw invalid("El rol " + Role.ADMIN_NAME
                    + " no se puede renombrar: la plataforma lo usa para que siempre haya quien la administre");
        }
    }

    // Agregarle permisos al Administrador sí se puede; quitarle los de Administración, no.
    public void checkCanRemovePermissions(Role role, Collection<RolePermission> removed) {
        if (!isAdminRole(role)) {
            return;
        }
        boolean losesAdministration = removed.stream()
                .anyMatch(rolePermission -> rolePermission.getPermission().getModule() == ModuleType.ADMINISTRACION);
        if (losesAdministration) {
            throw invalid("El rol " + Role.ADMIN_NAME + " debe conservar los permisos del módulo Administración: "
                    + "sin ellos nadie podría volver a gestionar usuarios, roles y permisos");
        }
    }

    // Es el último si está activo, tiene el rol y no hay otro usuario activo con él.
    private boolean isLastActiveAdmin(User user) {
        return user.isEnabled()
                && userRoleRepository.existsByUserIdAndRoleName(user.getId(), Role.ADMIN_NAME)
                && userRoleRepository.countEnabledUsersWithRole(Role.ADMIN_NAME) <= 1;
    }

    private static boolean isAdminRole(Role role) {
        return Role.ADMIN_NAME.equalsIgnoreCase(role.getName());
    }

    private static ApplicationException invalid(String message) {
        return new ApplicationException(ErrorCase.INVALID_OPERATION, message);
    }
}
