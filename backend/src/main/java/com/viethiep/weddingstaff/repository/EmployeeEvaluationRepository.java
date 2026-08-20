package com.viethiep.weddingstaff.repository;

import com.viethiep.weddingstaff.entity.EmployeeEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmployeeEvaluationRepository
        extends JpaRepository<EmployeeEvaluation, Long> {

    boolean existsByAssignment_Id(Long assignmentId);

    @Query("""
            select evaluation
            from EmployeeEvaluation evaluation
            join fetch evaluation.assignment assignment
            join fetch assignment.shift shift
            join fetch shift.event event
            join fetch event.venue
            join fetch evaluation.evaluatedBy
            where assignment.employee.id = :employeeId
            order by evaluation.evaluatedAt desc, evaluation.id desc
            """)
    List<EmployeeEvaluation> findAllByEmployeeId(
            @Param("employeeId") Long employeeId
    );
}
