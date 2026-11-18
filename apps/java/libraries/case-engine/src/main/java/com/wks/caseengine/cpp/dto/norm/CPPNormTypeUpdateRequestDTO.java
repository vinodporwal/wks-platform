package com.wks.caseengine.cpp.dto.norm;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CPPNormTypeUpdateRequestDTO {

    @JsonProperty("normsHeaderIds")
    private List<UUID> normsHeaderIds;

    // Source grid type: "norms" or "quantity"
    private String type;
}
