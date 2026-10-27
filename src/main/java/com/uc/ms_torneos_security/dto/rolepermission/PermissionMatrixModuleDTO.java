package com.uc.ms_torneos_security.dto.rolepermission;

import com.uc.ms_torneos_security.entity.ModuleType;
import lombok.Value;

import java.util.List;

@Value
public class PermissionMatrixModuleDTO {

    ModuleType module;

    // Una fila por recurso (model) del módulo.
    List<PermissionMatrixRowDTO> resources;
}
