package com.uc.ms_torneos_security.dto.rolepermission;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdatePermissionMatrixDTO {

    // Ids de las casillas que quedan marcadas al guardar. Una lista vacía le retira todos los permisos al rol.
    @NotNull(
            message = "La lista de permisos es obligatoria"
    )
    private List<
            @NotNull(message = "El identificador del permiso no puede ser nulo")
            Long> permissionIds;
}
