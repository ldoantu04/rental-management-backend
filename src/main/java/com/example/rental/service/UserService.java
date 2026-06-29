package com.example.rental.service;

import com.example.rental.domain.UserRole;
import com.example.rental.domain.UserStatus;
import com.example.rental.dto.EmployeeRequest;
import com.example.rental.dto.EmployeeResponse;
import com.example.rental.model.User;

import java.util.List;
import java.util.Set;

public interface UserService {
    User findByEmail(String email) throws Exception;
    User findByJwt(String jwt) throws Exception;

    List<EmployeeResponse> getAllEmployees();
    List<EmployeeResponse> searchEmployees(String keyword, UserRole vaiTro, UserStatus trangThai);
    EmployeeResponse getEmployeeById(Long id) throws Exception;
    EmployeeResponse createEmployee(EmployeeRequest req, User currentUser) throws Exception;
    EmployeeResponse updateEmployee(Long id, EmployeeRequest req, User currentUser) throws Exception;
    void deleteEmployee(Long id) throws Exception;
    List<EmployeeResponse.MotelSimple> getAvailableMotelsForEmployee(Long employeeId) throws Exception;
    Set<Long> getAssignedMotelIds(User user);
    boolean isAdmin(User user);
    boolean canAccessMotel(User user, Long motelId);
    boolean canAccessRoom(User user, Long roomId);
    boolean canAccessContract(User user, Long contractId);
    boolean canAccessInvoice(User user, Long invoiceId);
    boolean canAccessTenant(User user, Long tenantId);
}
