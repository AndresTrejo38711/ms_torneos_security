package com.uc.ms_torneos_security.dto.rolepermission;

import lombok.Value;

import java.util.List;

@Value
public class PermissionMatrixRowDTO {

    String model;

    // Siempre las 5 columnas, en orden: VIEW, LIST, CREATE, UPDATE, DELETE.
    List<PermissionMatrixCellDTO> operations;
}
