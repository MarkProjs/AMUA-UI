package com.example.backend.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.backend.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public List<Map<String, Object>> getCompanies() {
        return repository.getCompanies();
    }

    public List<Map<String, Object>> getAllBUs(String company) {
        return repository.getAllBUs(company);
    }

    public List<Map<String, Object>> getBUsForClass(String company, String clazz) {
        return repository.getBUsForClass(company, clazz);
    }

    public List<Map<String, Object>> getClassesForBU(String company) {
        return repository.getClassesForCompany(company);
    }

    public List<Map<String, Object>> getDirectorBasedOnParam(String company, String businessUnit, String clazz) {
        return repository.getDirectorBasedOnParam(company, businessUnit, clazz);
    }

    public List<Map<String, Object>> getAllDirectors() {
        return repository.getAllDirectors();
    }

    public List<Map<String, Object>> getListForCompanyOnly(String company) {
        return repository.getListForCompanyOnly(company);
    }

    public int updateNTAccount(String company, String businessUnit, String clazz, String newNTAccount) {
        return repository.updateNTAccount(company, businessUnit, clazz, newNTAccount);
    }

    public List<String> getNTAccountsByCompany(String company) {
        return repository.getNTAccountsByCompany(company);
    }

    // public int addClassForAll(String businessUnit, String clazz) {
    //     return repository.insertClassForAll(businessUnit, clazz);
    // }

    public int insertClassWithDirector(String company, String businessUnit, String clazz, String ntAccount) {
        return repository.insertClassWithDirector(company, businessUnit, clazz, ntAccount);
    }

    public int deleteClassForCompany(String clazz, String company) {
        return repository.deleteClassByCompany(clazz, company);
    }

    public int deleteRow(String clazz, String company) {
        return repository.deleteRow(clazz, company);
    }
}
