package com.uc.ms_torneos_security.entity;

// El orden de declaración es el orden en que aparecen los módulos en la matriz.
// La columna permissions.module guarda el nombre con máximo 20 caracteres (GESTION_COMPETENCIAS
// tiene justo 20). En MySQL esa columna es un ENUM: en una base que ya existía, un módulo nuevo
// hay que agregarlo a mano con ALTER TABLE (Hibernate no modifica columnas que ya existen).
public enum ModuleType {
    ADMINISTRACION,
    GESTION_DEPORTIVA,
    GESTION_COMPETENCIAS,
    COMERCIAL,
    INFRAESTRUCTURA,
    REPORTES
}
