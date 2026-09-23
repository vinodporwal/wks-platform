package com.wks.caseengine.dto;

import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@JsonInclude(Include.ALWAYS)
public class NetProductionDTO {

    private String sapMATCode;
    private String product;
    private String month;
    private Double actualQty;
    private String uom;

}
