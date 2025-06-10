package com.example.backend.repository;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> getCompanies() {
        return jdbcTemplate.queryForList("""
            SELECT DISTINCT Company FROM PBOAssetMgmt.dbo.AO_UserAssociation WHERE Role = 'director';
        """);
    }

    public List<Map<String, Object>> getAllClassesForCompany(String company) {
    return jdbcTemplate.queryForList("""
        SELECT DISTINCT Class FROM CorporateQuote.dbo.BUxClass WHERE Company = ? ORDER BY Class
    """, company);
    }

    public String getBusinessUnitForClass(String company, String clazz) {
        List<Map<String, Object>> result = jdbcTemplate.queryForList("""
            SELECT g.GroupNTDesc 
            FROM CorporateQuote.dbo.BUxClass b
            JOIN CorporateQuote.dbo.Groups g ON b.BU = g.GID
            WHERE b.Class = ? AND b.Company = ?
        """, clazz, company);
        
        return result.isEmpty() ? null : (String) result.get(0).get("GroupNTDesc");
    }
    
    public List<Map<String, Object>> getBUsForClass(String company, String clazz) {
        return jdbcTemplate.queryForList("""
            SELECT DISTINCT g.GroupNTDesc
            FROM PBOAssetMgmt.dbo.AO_UserAssociation a
            JOIN CorporateQuote.dbo.BUxClass b
                ON a.Class = b.Class AND a.Company = b.Company
            JOIN CorporateQuote.dbo.Groups g
                ON b.BU = g.GID
            WHERE a.Company = ? AND a.Class = ? AND a.Role = 'director';
        """, company, clazz);
    }

    public List<Map<String, Object>> getClassesForCompany(String company) {
        return jdbcTemplate.queryForList(""" 
            SELECT Class FROM PBOAssetMgmt.dbo.AO_UserAssociation WHERE Company = ? AND Role = 'director' ;  
        """, company);
    }

    public List<Map<String, Object>> getDirectorBasedOnParam(String company, String businessUnit, String clazz) {
        return jdbcTemplate.queryForList("""
            SELECT  DISTINCT a.Company, g.GroupNTDesc as BusinessUnit, a.Class, a.Role, a.NT_Account
            FROM PBOAssetMgmt.DBO.AO_UserAssociation a
            JOIN CorporateQuote.dbo.BUxClass b
                ON a.Class = b.Class AND a.Company = b.Company
            JOIN CorporateQuote.dbo.Groups g
                ON b.BU = g.GID
            WHERE a.Company = ? AND g.GroupNTDesc = ? AND a.Class = ? AND a.Role = 'director'
        """, company, businessUnit, clazz);
    }

    public List<Map<String, Object>> getAllDirectors() {
        return jdbcTemplate.queryForList("""
            SELECT DISTINCT a.Company, g.GroupNTDesc as BusinessUnit, a.Class, a.Role, a.NT_Account
            FROM PBOAssetMgmt.DBO.AO_UserAssociation a
            JOIN CorporateQuote.dbo.BUxClass b ON a.Class = b.Class AND a.Company = b.Company
            JOIN CorporateQuote.dbo.Groups g ON b.BU = g.GID
            WHERE a.Role = 'director';
        """);
    }

    public List<Map<String, Object>> getListForCompanyOnly(String company) {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT a.Company, g.GroupNTDesc as BusinessUnit, a.Class, a.Role, a.NT_Account
                FROM PBOAssetMgmt.DBO.AO_UserAssociation a
                JOIN CorporateQuote.dbo.BUxClass b ON a.Class = b.Class AND a.Company = b.Company
                JOIN CorporateQuote.dbo.Groups g ON b.BU = g.GID
                WHERE a.Company =? AND a.Role = 'director';
                """, company);
    }

    public int updateNTAccount(String company, String businessUnit, String clazz, String newNTAccount) {
        return jdbcTemplate.update("""
                UPDATE a SET a.NT_Account = ?
                FROM PBOAssetMgmt.DBO.AO_UserAssociation a
                JOIN CorporateQuote.dbo.BUxClass b ON a.Class = b.Class AND a.Company = b.Company
                JOIN CorporateQuote.dbo.Groups g ON b.BU = g.GID
                WHERE a.Company = ? AND a.Class = ? AND g.GroupNTDesc = ? AND a.Role = 'director';
                """, newNTAccount, company, clazz, businessUnit);
    }

    public List<String> getNTAccountsByCompany(String company) {
        String prefix;
        switch(company) {
            case "FUTA" -> prefix = "ASIA\\";
            case "FUTE" -> prefix = "EUR\\";
            case "FUTI" -> prefix = "NA\\";
            default -> throw new IllegalArgumentException("Unknown company: " + company);
        }

        String sql = "SELECT DISTINCT NT_Account FROM DATAWHSE..ADSUSERS WHERE NT_Account LIKE ? ORDER BY NT_Account";
        return jdbcTemplate.queryForList(sql, String.class, prefix + "%");
    }

    public int assignDirector(String company, String businessUnit, String clazz, String ntAccount) {
        String prefix;
        switch(company) {
            case "FUTA" -> prefix = "ASIA\\";
            case "FUTE" -> prefix = "EUR\\";
            case "FUTI" -> prefix = "NA\\";
            default -> throw new IllegalArgumentException("Unknown company: " + company);
        }

        // Ensure the NT_Account includes the correct prefix
        if (!ntAccount.startsWith(prefix)) {
            ntAccount = prefix + ntAccount.replaceFirst("^(ASIA|EUR|NA)\\\\", ""); // strip old prefix
        }

         // Validate class exists in BUxClass
        boolean existsInBUxClass = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM CorporateQuote.dbo.BUxClass WHERE Class = ? AND Company = ?
            ) THEN 1 ELSE 0 END
        """, Boolean.class, clazz, company));

        if (!existsInBUxClass) {
            return -1; // class not found
        }

        List<Map<String, Object>> gidList = jdbcTemplate.queryForList("""
                SELECT GID FROM CorporateQuote.dbo.Groups WHERE Company = ? AND GroupNTDesc = ?;
                """, company, businessUnit);

        if (gidList.isEmpty()) return 0;

        // Insert into AO_UserAssociation
        // Insert into AO_UserAssociation if not exists
        return jdbcTemplate.update("""
            IF NOT EXISTS (
                SELECT 1 FROM PBOAssetMgmt.dbo.AO_UserAssociation
                WHERE Company = ? AND Class = ? AND NT_Account = ? AND BU = ? AND Role = 'director'
            )
            BEGIN
                INSERT INTO PBOAssetMgmt.dbo.AO_UserAssociation (Company, Class, NT_Account, BU, Role)
                VALUES (?, ?, ?, ?, 'director')
            END
        """, company, clazz, ntAccount, businessUnit, company, clazz, ntAccount, businessUnit);
    }

    public int deleteClassByCompany(String clazz, String company){
        String sql = "DELETE FROM CorporateQuote.dbo.BUxClass WHERE Class = ? AND Company = ?";
        return jdbcTemplate.update(sql, clazz, company);
    }

    public int deleteRow(String clazz, String company) {
        String sql = "DELETE FROM PBOAssetMgmt.dbo.AO_UserAssociation WHERE Class = ? AND Company = ?";
        return jdbcTemplate.update(sql, clazz, company);
    }

}

