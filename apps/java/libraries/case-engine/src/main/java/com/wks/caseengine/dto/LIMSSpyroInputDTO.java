package com.wks.caseengine.dto;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Configuration
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class LIMSSpyroInputDTO {

    private String type;
    private String limsTagName;
    private String uom;

    private Double jmdNaphtha;
    private Double pmdNaphtha;
    private Double ioclNaphtha;
    private Double gailNaphtha;
    private Double bpclNaphtha;
    private Double ongcNaphtha;
    private Double otherNaphtha;

    private Double naphthaBlendCompositionForOptimizerInput;

    private String jmdNaphthaId;
    private String pmdNaphthaId;
    private String ioclNaphthaId;
    private String gailNaphthaId;
    private String bpclNaphthaId;
    private String ongcNaphthaId;
    private String otherNaphthaId;
    private String bcoiNaphthaId;
}

