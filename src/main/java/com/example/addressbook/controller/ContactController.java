package com.example.addressbook.controller;

import com.example.addressbook.model.Contact;
import com.example.addressbook.model.ContactMethod;
import com.example.addressbook.service.ContactService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/contacts")
// ❌ 删除 @CrossOrigin，改用全局配置
public class ContactController {

    @Autowired
    private ContactService contactService;

    @GetMapping
    public List<Contact> getAllContacts() {
        return contactService.getAllContacts();
    }

    @GetMapping("/bookmarked")
    public List<Contact> getBookmarkedContacts() {
        return contactService.getBookmarkedContacts();
    }

    @PostMapping
    public ResponseEntity<Contact> createContact(@RequestBody Contact contact) {
        Contact saved = contactService.createContact(contact);
        return ResponseEntity.ok(saved);
    }

    // ✅ 新增：更新整个联系人（用于编辑）
    @PutMapping("/{id}")
    public ResponseEntity<Contact> updateContact(@PathVariable Long id, @RequestBody Contact contact) {
        contact.setId(id); // 确保 ID 一致
        Contact updated = contactService.updateContact(contact);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/bookmark")
    public Contact toggleBookmark(@PathVariable Long id, @RequestBody Map<String, Boolean> request) {
        Boolean bookmarked = request.get("bookmarked");
        if (bookmarked == null) bookmarked = false;
        return contactService.updateBookmark(id, bookmarked);
    }

    @DeleteMapping("/{id}")
    public void deleteContact(@PathVariable Long id) {
        contactService.deleteContact(id);
    }

    // ✅ 导出为 Excel
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel() throws IOException {
        List<Contact> contacts = contactService.getAllContacts();
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Contacts");

        Row headerRow = sheet.createRow(0);
        String[] columnNames = {"ID", "Name", "Bookmarked", "Phone", "Email", "WeChat", "Address"};
        for (int i = 0; i < columnNames.length; i++) {
            headerRow.createCell(i).setCellValue(columnNames[i]);
        }

        int rowNum = 1;
        for (Contact contact : contacts) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(contact.getId());
            row.createCell(1).setCellValue(contact.getName());
            row.createCell(2).setCellValue(contact.getBookmarked());

            String phone = "", email = "", wechat = "", address = "";
            if (contact.getMethods() != null) {
                for (ContactMethod method : contact.getMethods()) {
                    String type = method.getMethodType().toLowerCase();
                    String val = method.getValue();
                    switch (type) {
                        case "phone" -> phone += val + "; ";
                        case "email" -> email += val + "; ";
                        case "wechat" -> wechat += val + "; ";
                        case "address" -> address += val + "; ";
                    }
                }
            }
            row.createCell(3).setCellValue(phone.stripTrailing());
            row.createCell(4).setCellValue(email.stripTrailing());
            row.createCell(5).setCellValue(wechat.stripTrailing());
            row.createCell(6).setCellValue(address.stripTrailing());
        }

        for (int i = 0; i < columnNames.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        byte[] bytes = out.toByteArray();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=contacts.xlsx")
                .body(bytes);
    }

    // ✅ 从 Excel 导入
    @PostMapping("/import")
    public ResponseEntity<String> importFromExcel(@RequestParam("file") MultipartFile file) {
        try {
            Workbook workbook = WorkbookFactory.create(file.getInputStream());
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String name = getStringCellValue(row.getCell(1));
                if (name == null || name.trim().isEmpty()) continue;

                Contact contact = new Contact(name);
                contact.setBookmarked(getBooleanCellValue(row.getCell(2)));

                List<ContactMethod> methods = new ArrayList<>();
                addMethods(methods, "phone", getStringCellValue(row.getCell(3)));
                addMethods(methods, "email", getStringCellValue(row.getCell(4)));
                addMethods(methods, "wechat", getStringCellValue(row.getCell(5)));
                addMethods(methods, "address", getStringCellValue(row.getCell(6)));
                contact.setMethods(methods);

                contactService.createContact(contact);
            }
            workbook.close();
            return ResponseEntity.ok("Import successful!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Import failed: " + e.getMessage());
        }
    }

    // 工具方法：安全获取单元格字符串值
    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType();
        if (type == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (type == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            if (val == Math.floor(val)) {
                return String.valueOf((long) val);
            } else {
                return String.valueOf(val);
            }
        } else if (type == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (type == CellType.FORMULA) {
            return cell.getCellFormula();
        }
        return null;
    }

    // 工具方法：安全获取布尔值
    private boolean getBooleanCellValue(Cell cell) {
        if (cell == null) return false;
        CellType type = cell.getCellType();
        if (type == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        } else if (type == CellType.STRING) {
            String str = cell.getStringCellValue();
            return "true".equalsIgnoreCase(str) || "1".equals(str);
        } else if (type == CellType.NUMERIC) {
            return cell.getNumericCellValue() != 0;
        }
        return false;
    }

    // 工具方法：解析分号分隔的多个值
    private void addMethods(List<ContactMethod> list, String type, String value) {
        if (value != null && !value.trim().isEmpty()) {
            Arrays.stream(value.split(";"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(v -> list.add(new ContactMethod(type, v)));
        }
    }
}