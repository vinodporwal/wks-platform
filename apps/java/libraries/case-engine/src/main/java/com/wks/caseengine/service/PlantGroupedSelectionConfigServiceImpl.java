package com.wks.caseengine.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class PlantGroupedSelectionConfigServiceImpl implements PlantGroupedSelectionConfigService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public AOPMessageVM checkMaterialGroupedSelectionPopup(String plantId) {
        try {
            String sql = "EXEC [dbo].[SP_CheckMaterialGroupedSelectionPopup] @plantId = ?";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, plantId);
            
            boolean isOpenPopup = false;
            if (rows != null && !rows.isEmpty()) {
                Object val = rows.get(0).get("IsOpenPopup");
                if (val instanceof Boolean) {
                    isOpenPopup = (Boolean) val;
                } else if (val instanceof Number) {
                    isOpenPopup = ((Number) val).intValue() == 1;
                } else if (val != null) {
                    isOpenPopup = Boolean.parseBoolean(val.toString()) || "1".equals(val.toString());
                }
            }
            
            return AOPMessageVM.builder()
                    .code(200)
                    .message("Popup status fetched successfully")
                    .data(isOpenPopup)
                    .build();
        } catch (Exception e) {
            return AOPMessageVM.builder()
                    .code(500)
                    .message("Error checking popup status: " + e.getMessage())
                    .data(false)
                    .build();
        }
    }
}
