package com.uc.ms_torneos_security.dto.permission;

import com.uc.ms_torneos_security.entity.ModuleType;
import com.uc.ms_torneos_security.entity.Operation;
import lombok.Value;

// Permiso sin sus endpoints: lo justo para que el front sepa qué opciones mostrar.
@Value
public class PermissionSummaryDTO {

    ModuleType module;

    String model;

    Operation operation;
}
