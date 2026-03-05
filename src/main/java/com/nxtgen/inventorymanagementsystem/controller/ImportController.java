package com.nxtgen.inventorymanagementsystem.controller;

import com.nxtgen.inventorymanagementsystem.entity.Item;
import com.nxtgen.inventorymanagementsystem.repository.ItemRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ItemRepository itemRepository;

    public ImportController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @PostMapping("/excel")
    public ResponseEntity<?> importExcel(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".xlsx")) {
            return ResponseEntity.badRequest().body("Please upload an .xlsx file.");
        }

        int inserted = 0;
        int updated = 0;
        List<String> errors = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook wb = new XSSFWorkbook(is)) {

            Sheet sheet = wb.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) {
                return ResponseEntity.badRequest().body("No data rows found.");
            }

            Row header = sheet.getRow(0);
            Map<String, Integer> col = new HashMap<>();
            for (Cell c : header) {
                String key = getStringCell(c).trim().toLowerCase();
                if (!key.isBlank()) col.put(key, c.getColumnIndex());
            }

            for (String req : List.of("name", "category", "location", "quantity")) {
                if (!col.containsKey(req)) {
                    return ResponseEntity.badRequest().body("Missing required column: " + req);
                }
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String itemName = getStringCell(row.getCell(col.get("name"))).trim();
                String category = getStringCell(row.getCell(col.get("category"))).trim();
                String location = getStringCell(row.getCell(col.get("location"))).trim();
                Integer quantity = getIntCell(row.getCell(col.get("quantity")));

                if (itemName.isBlank() && category.isBlank() && location.isBlank() && (quantity == null)) {
                    continue;
                }

                if (itemName.isBlank() || category.isBlank() || location.isBlank() || quantity == null || quantity < 0) {
                    errors.add("Row " + (r + 1) + ": invalid data (name/category/location required, quantity >= 0).");
                    continue;
                }

                Optional<Item> existingOpt = itemRepository.findByNameIgnoreCaseAndLocationIgnoreCase(itemName, location);

                if (existingOpt.isPresent()) {
                    Item existing = existingOpt.get();
                    existing.setCategory(category);
                    existing.setQuantity(quantity);
                    itemRepository.save(existing);
                    updated++;
                } else {
                    Item it = new Item();
                    it.setName(itemName);
                    it.setCategory(category);
                    it.setLocation(location);
                    it.setQuantity(quantity);
                    itemRepository.save(it);
                    inserted++;
                }
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Import failed: " + e.getMessage());
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("errors", errors);

        return ResponseEntity.ok(result);
    }

    private static String getStringCell(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue();
    }

    private static Integer getIntCell(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            String s = getStringCell(cell).trim();
            if (s.isBlank()) return null;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }
}
