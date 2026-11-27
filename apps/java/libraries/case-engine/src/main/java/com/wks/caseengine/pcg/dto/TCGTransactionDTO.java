package com.wks.caseengine.pcg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TCGTransactionDTO {
    
     private String normparameterFkId;
     private String name;
     private String displayName;
     private String uom;
     private String normParameterType;
     private String config;
     private String type;
     private String apr;
     private String may;
     private String jun;
     private String jul;
     private String aug;
     private String sep;
     private String oct;
     private String nov;
     private String dec;
     private String jan;
     private String feb;
     private String mar;
     private String remarks;
     private Integer displayOrder;
     private Boolean isEditable;

}
