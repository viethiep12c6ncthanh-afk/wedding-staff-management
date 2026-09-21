package com.viethiep.weddingstaff.dto;

public record AiStatusResponse(boolean enabled, String provider, String model,
                               boolean connectionTestAvailable) {
}
