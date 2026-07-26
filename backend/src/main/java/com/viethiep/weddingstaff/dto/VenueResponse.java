package com.viethiep.weddingstaff.dto;

import com.viethiep.weddingstaff.enumtype.CommonStatus;

public record VenueResponse(Long id, String name, String address, String contactPhone, CommonStatus status) {}
