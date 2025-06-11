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

    public List<Map<String, Object>> getDisplayNameAndNTAccounts(String company) {
        String prefix = switch (company) {
            case "FUTA" -> "ASIA\\";
            case "FUTE" -> "EUR\\";
            case "FUTI" -> "NA\\";
            default      -> throw new IllegalArgumentException("Unknown company: " + company);
        };

        return jdbcTemplate.queryForList("""
            SELECT  DisplayName AS label,
                    NT_Account  AS value
            FROM    DATAWHSE..ADSUSERS
            WHERE   NT_Account LIKE ?
            ORDER BY DisplayName
        """, prefix + "%");
    }



    public int assignDirector(String company,
                            String businessUnit,
                            String clazz,
                            String ntAccount) {

        // -------- 1. prefix handling ----------
        String prefix = switch (company) {
            case "FUTA" -> "ASIA\\";
            case "FUTE" -> "EUR\\";
            case "FUTI" -> "NA\\";
            default     -> throw new IllegalArgumentException("Unknown company: " + company);
        };
        if (!ntAccount.startsWith(prefix)) {
            ntAccount = prefix + ntAccount.replaceFirst("^(ASIA|EUR|NA)\\\\", "");
        }

        // -------- 2. class must exist ----------
        boolean classExists = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM CorporateQuote.dbo.BUxClass
                WHERE Company = ? AND Class = ?
            ) THEN 1 ELSE 0 END
        """, Boolean.class, company, clazz));
        if (!classExists) return -1;      // class missing in BUxClass

        // -------- 3. BU must map to a GID ----------
        boolean gidExists = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT CASE WHEN EXISTS (
                SELECT 1
                FROM CorporateQuote.dbo.Groups
                WHERE Company = ? AND GroupNTDesc = ?
            ) THEN 1 ELSE 0 END
        """, Boolean.class, company, businessUnit));

        if (!gidExists) return 0; // BU description not found

        // -------- 4. *NEW* : any director already on this Class+BU ? ----------
        boolean directorExists = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM PBOAssetMgmt.dbo.AO_UserAssociation
                WHERE Company = ? AND Class = ? AND BU = ? AND Role = 'director'
            ) THEN 1 ELSE 0 END
        """, Boolean.class, company, clazz, businessUnit));
        if (directorExists) return 3;     // << we’ll treat 3 as “duplicate director”

        // -------- 5. same-class-different-BU conflict ----------
        boolean diffBuConflict = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
            SELECT CASE WHEN EXISTS (
                SELECT 1 FROM PBOAssetMgmt.dbo.AO_UserAssociation
                WHERE Company = ? AND Class = ? AND BU <> ? AND Role = 'director'
            ) THEN 1 ELSE 0 END
        """, Boolean.class, company, clazz, businessUnit));
        if (diffBuConflict) return 2;     // original “class exists in a different BU”

        // -------- 6. insert if the exact row is new ----------
        return jdbcTemplate.update("""
            IF NOT EXISTS (
                SELECT 1 FROM PBOAssetMgmt.dbo.AO_UserAssociation
                WHERE Company = ? AND Class = ? AND NT_Account = ? AND BU = ? AND Role = 'director'
            )
            INSERT INTO PBOAssetMgmt.dbo.AO_UserAssociation
                (Company, Class, NT_Account, BU, Role)
            VALUES (?, ?, ?, ?, 'director')
        """, company, clazz, ntAccount, businessUnit,   // IF
            company, clazz, ntAccount, businessUnit);  // INSERT
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