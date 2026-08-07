package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.Employee;
import com.viethiep.weddingstaff.enumtype.AccountStatus;
import com.viethiep.weddingstaff.enumtype.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUserUsername(String username);

    boolean existsByEmployeeCode(String employeeCode);

    long countByEmploymentStatus(EmployeeStatus employmentStatus);

    @Query("""
            select employee
            from Employee employee
            join fetch employee.user u
            where employee.id = :id
            """)
    Optional<Employee> findByIdWithUser(@Param("id") Long id);

    @Query("""
            select employee
            from Employee employee
            join fetch employee.user u
            where (
                :keyword is null
                or lower(employee.employeeCode) like lower(concat('%', :keyword, '%'))
                or lower(u.username) like lower(concat('%', :keyword, '%'))
                or lower(u.fullName) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(u.email, '')) like lower(concat('%', :keyword, '%'))
                or lower(coalesce(u.phone, '')) like lower(concat('%', :keyword, '%'))
            )
            and (:employmentStatus is null or employee.employmentStatus = :employmentStatus)
            and (:accountStatus is null or u.accountStatus = :accountStatus)
            order by employee.employeeCode
            """)
    List<Employee> search(
            @Param("keyword") String keyword,
            @Param("employmentStatus") EmployeeStatus employmentStatus,
            @Param("accountStatus") AccountStatus accountStatus
    );
}
