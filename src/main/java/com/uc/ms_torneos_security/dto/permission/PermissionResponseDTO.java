package com.uc.ms_torneos_security.dto.permission;

import com.uc.ms_torneos_security.entity.ModuleType;
import com.uc.ms_torneos_security.entity.Operation;
import lombok.Value;

import java.util.List;

@Value
public class PermissionResponseDTO {

    Long id;

    ModuleType module;

    String model;

    Operation operation;

    List<EndpointResponseDTO> endpoints;
}
