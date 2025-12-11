package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.wks.caseengine.dto.TCSSlowdownDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.TCSSlowdownService;

@RestController
@RequestMapping("task")
public class TCSSlowdownController {

    @Autowired
    private TCSSlowdownService tcsslowdownService;

    @GetMapping("/tcs-slowdown")
    public AOPMessageVM getAllTCSSlowdown() {
        return tcsslowdownService.getAll();
    }

    @PostMapping("/tcs-slowdown")
    public AOPMessageVM saveOrUpdate(@RequestBody List<TCSSlowdownDTO> payload) {
        return tcsslowdownService.saveOrUpdate(payload);
    }
}
