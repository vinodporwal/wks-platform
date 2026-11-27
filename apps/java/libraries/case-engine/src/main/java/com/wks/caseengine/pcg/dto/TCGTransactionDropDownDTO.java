package com.wks.caseengine.pcg.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class TCGTransactionDropDownDTO {
    
    private String name;
    private String displayName;
}
