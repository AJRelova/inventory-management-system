package com.nxtgen.inventorymanagementsystem.controller;

import com.nxtgen.inventorymanagementsystem.entity.InventoryHistory;
import com.nxtgen.inventorymanagementsystem.entity.Item;
import com.nxtgen.inventorymanagementsystem.repository.InventoryHistoryRepository;
import com.nxtgen.inventorymanagementsystem.repository.ItemRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@RestController
@RequestMapping("/api/import")
public class ImportController {

    private final ItemRepository itemRepository;
    private final InventoryHistoryRepository historyRepository;

    public ImportController(ItemRepository itemRepository, InventoryHistoryRepository historyRepository) {
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
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

        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet.getPhysicalNumberOfRows() < 2) {
                return ResponseEntity.badRequest().body("No data rows found.");
            }

            Row header = sheet.getRow(0);
            Map<String, Integer> col = new HashMap<>();
            for (Cell c : header) {
                String key = getStringCell(c).trim().toLowerCase();
                if (!key.isBlank()) {
                    col.put(key, c.getColumnIndex());
                }
            }

            for (String req : List.of("serial number", "description", "category", "location", "quantity")) {
                if (!col.containsKey(req)) {
                    return ResponseEntity.badRequest().body("Missing required column: " + req);
                }
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                String serialNumber = getStringCell(row.getCell(col.get("serial number"))).trim();
                String description = getStringCell(row.getCell(col.get("description"))).trim();
                String category = getStringCell(row.getCell(col.get("category"))).trim();
                String location = getStringCell(row.getCell(col.get("location"))).trim();
                Integer quantity = getIntCell(row.getCell(col.get("quantity")));
                String deliveryReceipt = getOptionalCell(row, col, "delivery receipt");
                String hardwareRevision = getOptionalCell(row, col, "hardware revision");
                String vendor = getOptionalCell(row, col, "vendor");

                if (serialNumber.isBlank() && description.isBlank() && category.isBlank() && location.isBlank() && quantity == null) {
                    continue;
                }

                if (serialNumber.isBlank() || description.isBlank() || category.isBlank() || location.isBlank() || quantity == null || quantity < 0) {
                    errors.add("Row " + (r + 1) + ": invalid data (serial number/description/category/location required, quantity >= 0).");
                    continue;
                }

                Optional<Item> existingOpt = itemRepository.findBySerialNumberIgnoreCase(serialNumber);

                if (existingOpt.isPresent()) {
                    Item existing = existingOpt.get();
                    int change = quantity - existing.getQuantity();

                    existing.setDescription(description);
                    existing.setCategory(category);
                    existing.setLocation(location);
                    existing.setQuantity(quantity);
                    existing.setDeliveryReceipt(deliveryReceipt);
                    existing.setHardwareRevision(hardwareRevision);
                    existing.setVendor(vendor);
                    existing.setLastEditedBy(currentUsername());

                    itemRepository.save(existing);
                    historyRepository.save(new InventoryHistory(existing.getId(), existing.getDescription(), "IMPORT_UPDATE", change));
                    updated++;
                } else {
                    Item it = new Item();
                    it.setSerialNumber(serialNumber);
                    it.setDescription(description);
                    it.setCategory(category);
                    it.setLocation(location);
                    it.setQuantity(quantity);
                    it.setDeliveryReceipt(deliveryReceipt);
                    it.setHardwareRevision(hardwareRevision);
                    it.setVendor(vendor);
                    it.setLastEditedBy(currentUsername());

                    Item saved = itemRepository.save(it);
                    historyRepository.save(new InventoryHistory(saved.getId(), saved.getDescription(), "IMPORT_ADD", saved.getQuantity()));
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

    @GetMapping("/excel")
    public ResponseEntity<byte[]> exportExcel() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Inventory");

            String[] headers = {
                    "Serial Number",
                    "Description",
                    "Category",
                    "Location",
                    "Quantity",
                    "Delivery Receipt",
                    "Hardware Revision",
                    "Vendor",
                    "Last Edited By"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            List<Item> items = itemRepository.findAll();
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
                Row row = sheet.createRow(i + 1);

                row.createCell(0).setCellValue(nvl(item.getSerialNumber()));
                row.createCell(1).setCellValue(nvl(item.getDescription()));
                row.createCell(2).setCellValue(nvl(item.getCategory()));
                row.createCell(3).setCellValue(nvl(item.getLocation()));
                row.createCell(4).setCellValue(item.getQuantity());
                row.createCell(5).setCellValue(nvl(item.getDeliveryReceipt()));
                row.createCell(6).setCellValue(nvl(item.getHardwareRevision()));
                row.createCell(7).setCellValue(nvl(item.getVendor()));
                row.createCell(8).setCellValue(nvl(item.getLastEditedBy()));
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-export.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private static String getStringCell(Cell cell) {
        if (cell == null) {
            return "";
        }
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue();
    }

    private static Integer getIntCell(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }

            String s = getStringCell(cell).trim();
            if (s.isBlank()) {
                return null;
            }
            return Integer.parseInt(s);

        } catch (Exception e) {
            return null;
        }
    }

    private static String getOptionalCell(Row row, Map<String, Integer> col, String key) {
        Integer idx = col.get(key);
        return idx == null ? "" : getStringCell(row.getCell(idx)).trim();
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "unknown";
    }
}
