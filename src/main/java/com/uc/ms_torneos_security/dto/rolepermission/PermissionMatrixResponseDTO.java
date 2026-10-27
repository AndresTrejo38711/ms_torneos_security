package com.uc.ms_torneos_security.dto.rolepermission;

import lombok.Value;

import java.util.List;

@Value
public class PermissionMatrixResponseDTO {

    Long roleId;

    String roleName;

    // Un grupo por módulo, y dentro de cada uno sus recursos.
    List<PermissionMatrixModuleDTO> modules;
}
