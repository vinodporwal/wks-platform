package com.wks.caseengine.rest.tcs;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.wks.caseengine.tcs.dto.TCSNetCapacityDTO;
import com.wks.caseengine.tcs.dto.TCSNetCapacityUOMDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.tcs.service.TCSNetCapacityService;

@RestController
@RequestMapping("task")
public class TCSNetCapacityController {

    @Autowired
    private TCSNetCapacityService tcsNetCapacityService;

    @GetMapping("/tcs-net-capacity")
    public Map<String, Object> getAllTCSNetCapacity(
        @RequestParam(required = false) String plantId,
        @RequestParam String year,
        @RequestParam String capacityType,
      //  @RequestParam(required = false) String uom,
        @RequestParam(required = false) String siteId,
        @RequestParam(required = false) String verticalId)
       
        {
            //if PlantId is null, then siteId must not be null
if (plantId == null && (siteId == null || verticalId == null)) {
    throw new RestInvalidArgumentException("Plant ID and Site ID or Vertical ID cannot be null", null);
}
if (plantId != null && (siteId != null || verticalId != null)) {
    throw new RestInvalidArgumentException("Plant ID and Site ID or Vertical ID cannot be provided together", null);
}

        return tcsNetCapacityService.getAll(
            plantId,
            year,
            capacityType,
      //      uom,
            siteId,
            verticalId);
    }

    // @PostMapping("/tcs-net-capacity")
    // public AOPMessageVM saveOrUpdate(
    //     @RequestParam String plantId,
    //     @RequestParam String year,
    //     @RequestParam String capacityType,
    //  //   @RequestParam(required = false) String uom,
    //     @RequestBody List<TCSNetCapacityDTO> payload) {

    //     return tcsNetCapacityService.saveOrUpdate(
    //         plantId,
    //         year,
    //         capacityType,
    //     //    uom,
    //         payload);
    // }

    @GetMapping("/tcs-net-capacity/uom")
    public List<TCSNetCapacityUOMDTO> getAllUOM(
        @RequestParam (required = false) String plantId,
        @RequestParam String year,
        @RequestParam String capacityType,
        @RequestParam(required = false) String verticalId
    ) {

        if (plantId == null &&  verticalId == null) {
            throw new RestInvalidArgumentException("Plant ID and Vertical ID cannot be null", null);
        }
        if (plantId != null && verticalId != null) {
            throw new RestInvalidArgumentException("Plant ID and Vertical ID cannot be provided together", null);
        }

        return tcsNetCapacityService.getAllUOM(
            plantId,
            year,
            capacityType,
            verticalId);
    }
}

