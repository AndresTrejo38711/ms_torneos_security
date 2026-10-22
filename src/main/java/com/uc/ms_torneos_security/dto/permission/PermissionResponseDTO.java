package com.uc.ms_torneos_security.dto.permission;

import com.uc.ms_torneos_security.entity.HttpMethodType;
import com.uc.ms_torneos_security.entity.Operation;
import lombok.Value;

@Value
public class PermissionResponseDTO {

    Long id;

    String model;

    Operation operation;

    String url;

    HttpMethodType method;
}
