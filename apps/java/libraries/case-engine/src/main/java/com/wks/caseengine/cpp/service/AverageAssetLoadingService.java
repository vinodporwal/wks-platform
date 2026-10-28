package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface AverageAssetLoadingService {

    AOPMessageVM getAverageAssetLoading(List<UUID> plantIds, String aopYear);

    byte[] exportAverageAssetLoading(List<UUID> plantIds, String aopYear) throws IOException;
}
