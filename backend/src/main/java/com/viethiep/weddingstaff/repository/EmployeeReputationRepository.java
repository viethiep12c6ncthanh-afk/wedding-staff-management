package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.EmployeeReputation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeReputationRepository
        extends JpaRepository<EmployeeReputation, Long> {

    @Query("""
            select reputation
            from EmployeeReputation reputation
            join fetch reputation.employee employee
            join fetch employee.user
            order by reputation.currentScore desc, employee.employeeCode asc
            """)
    List<EmployeeReputation> findAllWithEmployee();

    @Query("""
            select reputation
            from EmployeeReputation reputation
            join fetch reputation.employee employee
            join fetch employee.user
            where employee.id = :employeeId
            """)
    Optional<EmployeeReputation> findByEmployeeIdWithEmployee(
            @Param("employeeId") Long employeeId
    );

    @Query("""
            select reputation
            from EmployeeReputation reputation
            join fetch reputation.employee employee
            join fetch employee.user user
            where user.username = :username
            """)
    Optional<EmployeeReputation> findByEmployeeUsernameWithEmployee(
            @Param("username") String username
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select reputation
            from EmployeeReputation reputation
            join fetch reputation.employee employee
            join fetch employee.user
            where employee.id = :employeeId
            """)
    Optional<EmployeeReputation> findByEmployeeIdForUpdate(
            @Param("employeeId") Long employeeId
    );
}
