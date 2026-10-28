package com.uc.ms_torneos_security.dto.permission;

import com.uc.ms_torneos_security.entity.HttpMethodType;
import lombok.Value;

@Value
public class EndpointResponseDTO {

    String url;

    HttpMethodType method;
}
