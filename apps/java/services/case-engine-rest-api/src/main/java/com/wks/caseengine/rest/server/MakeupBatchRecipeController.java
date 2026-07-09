package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.service.MakeupBatchRecipeService;
import com.wks.caseengine.dto.ChemGradeDTO;
import com.wks.caseengine.dto.MakeupBatchRecipeCalcDTO;
import com.wks.caseengine.dto.MakeupBatchRecipeDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("task")
public class MakeupBatchRecipeController {
    
    @Autowired
    private MakeupBatchRecipeService makeupBatchRecipeService;

    @GetMapping("/makeup-batch-recipe-data")
    public AOPMessageVM getMakeupBatchRecipeData(@RequestParam String plantId, @RequestParam String aopYear) {
        return makeupBatchRecipeService.getMakeupBatchRecipeData(plantId, aopYear);
    }

    @PostMapping("/makeup-batch-recipe-data")
    public AOPMessageVM saveMakeupBatchRecipeData(@RequestParam String plantId, @RequestParam String aopYear, @RequestBody List<MakeupBatchRecipeDTO> dtoList) {
        return makeupBatchRecipeService.saveMakeupBatchRecipeData(plantId, aopYear, dtoList);
    }

    @GetMapping("/makeup-batch-recipe-calc-data")
    public AOPMessageVM getMakeupBatchRecipeCalcData(@RequestParam String plantId, @RequestParam String aopYear) {
        return makeupBatchRecipeService.getMakeupBatchRecipeCalcData(plantId, aopYear);
    }

    @PostMapping("/makeup-batch-recipe-calc-data")
    public AOPMessageVM saveMakeupBatchRecipeCalcData(@RequestParam String plantId, @RequestParam String aopYear, @RequestBody List<MakeupBatchRecipeCalcDTO> dtoList) {
        return makeupBatchRecipeService.saveMakeupBatchRecipeCalcData(plantId, aopYear, dtoList);
    }

    @GetMapping("/chem-grade-data")
    public AOPMessageVM getChemGradeData(@RequestParam String plantId, @RequestParam String aopYear) {
        return makeupBatchRecipeService.getChemGradeData(plantId, aopYear);
    }

    @PostMapping("/chem-grade-data")
    public AOPMessageVM saveChemGradeData(@RequestParam String plantId, @RequestParam String aopYear, @RequestBody List<ChemGradeDTO> dtoList) {
        return makeupBatchRecipeService.saveChemGradeData(plantId, aopYear, dtoList);
    }

    @GetMapping("/final-calculated-cat-chem")
    public AOPMessageVM getFinalCalculatedCatChem(@RequestParam String plantId, @RequestParam String aopYear) {
        return makeupBatchRecipeService.getFinalCalculatedCatChem(plantId, aopYear);
    }

    @GetMapping("/makeup-batch-recipe-export")
    public ResponseEntity<byte[]> exportMakeupBatchRecipe(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        try {
            byte[] excelBytes = makeupBatchRecipeService.createMakeupBatchRecipeExcel(plantId, aopYear, false, null);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("makeup_batch_recipe.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/makeup-batch-recipe-import", consumes = "multipart/form-data")
    public AOPMessageVM importMakeupBatchRecipe(
            @RequestParam String plantId,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        return makeupBatchRecipeService.importMakeupBatchRecipeExcel(plantId, aopYear, file);
    }

    @GetMapping("/chem-grade-export")
    public ResponseEntity<byte[]> exportChemGrade(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        try {
            byte[] excelBytes = makeupBatchRecipeService.createChemGradeExcel(plantId, aopYear, false, null);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("chem_grade.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/chem-grade-import", consumes = "multipart/form-data")
    public AOPMessageVM importChemGrade(
            @RequestParam String plantId,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        return makeupBatchRecipeService.importChemGradeExcel(plantId, aopYear, file);
    }
}
