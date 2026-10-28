package com.wks.caseengine.tcs.repository;

import java.util.UUID;

public interface FurnaceProjection {

    UUID getId();
    String getName();   // was getFurnace() — matches renamed TCS_Furnace.Name column
    Double getJan();
    Double getFeb();
    Double getMar();
    Double getApr();
    Double getMay();
    Double getJun();
    Double getJul();
    Double getAug();
    Double getSep();
    Double getOct();
    Double getNov();
    Double getDec();
    String getRemarks();
}
