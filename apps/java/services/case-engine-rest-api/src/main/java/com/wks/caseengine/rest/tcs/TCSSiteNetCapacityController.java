package com.wks.caseengine.rest.tcs;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.wks.caseengine.tcs.dto.TCSSiteNetCapacityDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.tcs.service.TCSSiteNetCapacityService;

@RestController
@RequestMapping("task")
public class TCSSiteNetCapacityController {
    @Autowired
    private TCSSiteNetCapacityService tcsSiteNetCapacityService;

    @GetMapping("/site-capacity")
    public Map<String, Object> getAllTCSSiteNetCapacity(
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

        return tcsSiteNetCapacityService.getAll(
            plantId,
            year,
            capacityType,
      //      uom,
            siteId,
            verticalId);

}
}

