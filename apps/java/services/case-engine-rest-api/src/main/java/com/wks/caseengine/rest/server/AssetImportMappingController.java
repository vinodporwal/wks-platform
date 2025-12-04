package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.wks.caseengine.dto.AssetImportMappingDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.AssetImportMappingService;

@RestController
@RequestMapping("task")
public class AssetImportMappingController {

    @Autowired
    private AssetImportMappingService assetImportMappingService;

    
    @GetMapping("/asset-import-mapping")
    public AOPMessageVM getAllAssetImportMapping() {
        return assetImportMappingService.getAll();
    }

    
    @PostMapping("/asset-import-mapping")
    public AOPMessageVM saveOrUpdate(@RequestBody List<AssetImportMappingDTO> payload) {
        return assetImportMappingService.saveOrUpdate(payload);
    }

}
