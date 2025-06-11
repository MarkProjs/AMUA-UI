package com.example.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.service.UserService;





@RestController
@RequestMapping("api/users")
public class UserController {
    
    private final UserService userService;

    public UserController(UserService userSearchService) {
        this.userService = userSearchService;
    }

    @GetMapping("/companies")
    public List<Map<String, Object>> getCompanies() {
        return userService.getCompanies();
    }

    @GetMapping("/bu/class")
    public List<Map<String, Object>> getBUsForClass(@RequestParam String company,
                                            @RequestParam String clazz) {
        return userService.getBUsForClass(company, clazz);
    }

    @GetMapping("/bu/for-class")
    public ResponseEntity<Map<String, String>> getBusinessUnitForClass(@RequestParam String company, 
                                                                   @RequestParam String clazz) {
        String businessUnit = userService.getBusinessUnitForClass(company, clazz);
        if (businessUnit == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("businessUnit", businessUnit));
    }

    @GetMapping("/classes")
    public List<Map<String, Object>> getClassesForCompany(@RequestParam String company) {
        return userService.getClassesForBU(company);
    }

    @GetMapping("/search/param")
    public List<Map<String, Object>> searchDirector(@RequestParam String company,
                                            @RequestParam String clazz,
                                            @RequestParam String businessUnit) {
        return userService.getDirectorBasedOnParam(company, businessUnit, clazz);
    }

    @GetMapping("/search/all")
    public List<Map<String, Object>> searchAll() {
        return userService.getAllDirectors();
    }
    
    @GetMapping("/search/company")
    public List<Map<String, Object>> searchByCompany(@RequestParam String company) {
        return userService.getListForCompanyOnly(company);
    }

    @GetMapping("/classes/all")
    public List<Map<String, Object>> getAllClassesForCompany(@RequestParam String company) {
        return userService.getAllClassesForCompany(company);
    }

    @GetMapping("/ntaccounts")
    public ResponseEntity<List<Map<String, Object>>> getNtAccounts(@RequestParam String company) {
        return ResponseEntity.ok(userService.getNTAccountsByCompany(company));
    }


    @PutMapping("/update-nt")
    public Map<String, Object> updateNTAccount(@RequestBody Map<String, String> payload) {
        String company = payload.get("company");
        String businessUnit = payload.get("businessUnit");
        String clazz = payload.get("clazz");
        String newNTAccount = payload.get("ntAccount");

        int rowsAffected = userService.updateNTAccount(company, businessUnit, clazz, newNTAccount);
        return Map.of("rowsUpdated", rowsAffected);
    }


    @PostMapping("/add-director")
    public ResponseEntity<Map<String, Object>> addDirector(@RequestBody Map<String, String> p) {

        int code = userService.assignDirector(
                    p.get("company"),
                    p.get("businessUnit"),
                    p.get("clazz"),
                    p.get("ntAccount"));

        return switch (code) {
            case  1 -> ResponseEntity.ok(Map.of("rowsInserted", 1));
            case  0 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                                    .body(Map.of("message", "Business Unit not found."));
            case -1 -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                                    .body(Map.of("message", "Class not found."));
            case  2 -> ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body(Map.of("message", "Class already associated to a different Business Unit."));
            case  3 -> ResponseEntity.status(HttpStatus.CONFLICT)
                                    .body(Map.of("message", "A director already exists for this Class ."));
            default -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body(Map.of("message", "Unhandled result code: " + code));
        };
    }


    @DeleteMapping("/delete/bu")
    public Map<String, Object> deleteClassForCompany(@RequestParam String clazz, 
                                                @RequestParam String company) {
        int deletedCount = userService.deleteClassForCompany(clazz, company);
        return Map.of("rowsDeleted", deletedCount);
    }
    
    @DeleteMapping("/delete/row")
    public Map<String, Object> deleteRowFromTable(@RequestParam String clazz,
                                            @RequestParam String company) {
        int deletedRow = userService.deleteRow(clazz, company);
        return Map.of("rowsDeleted", deletedRow);
    }
}