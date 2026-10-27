package com.uc.ms_torneos_security.dto.rolepermission;

import com.uc.ms_torneos_security.entity.Operation;
import lombok.Value;

@Value
public class PermissionMatrixCellDTO {

    Operation operation;

    // null cuando ese permiso no está registrado para el recurso: la casilla no se puede marcar.
    Long permissionId;

    boolean assigned;
}
