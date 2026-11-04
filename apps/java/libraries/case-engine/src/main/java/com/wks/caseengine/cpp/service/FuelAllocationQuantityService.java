package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface FuelAllocationQuantityService {

    AOPMessageVM getFuelAllocationQuantity(List<UUID> plantIds, String aopYear);

    byte[] exportFuelAllocationQuantity(List<UUID> plantIds, String aopYear) throws IOException;
}

