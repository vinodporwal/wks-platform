package com.wks.caseengine.cpp.dto.norm;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NormParameterDTO {

    private UUID id;

    private String name;

    private String displayName;

    private String uom;

    private String expression;

    private String executeQuery;

    private UUID dependantAttributeId;

    private String type;

    private UUID normParameterTypeFkId;

    @JsonProperty("plantFkId")
    private UUID plantFkId;

    private Integer normTypeFkId;

    private Boolean isHistorical;

    private Integer displayOrder;

    private Boolean isEditable;

    private Boolean isVisible;

    private String calculationType;

    private String sapMaterialCode;

    private String normTypeName;
}
