package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUserUsername(String username);

    boolean existsByEmployeeCode(String employeeCode);

    @Query("""
            select employee
            from Employee employee
            join fetch employee.user
            order by employee.employeeCode
            """)
    List<Employee> findAllWithUser();
}
