package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.wks.caseengine.dto.TCSShutdownDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.TCSShutdownService;

@RestController
@RequestMapping("task")
public class TCSShutdownController {

    @Autowired
    private TCSShutdownService tcsShutdownService;

    @GetMapping("/tcs-shutdown")
    public AOPMessageVM getAllTCSShutdown() {
        return tcsShutdownService.getAll();
    }

    @PostMapping("/tcs-shutdown")
    public AOPMessageVM saveOrUpdate(@RequestBody List<TCSShutdownDTO> payload) {
        return tcsShutdownService.saveOrUpdate(payload);
    }
}
