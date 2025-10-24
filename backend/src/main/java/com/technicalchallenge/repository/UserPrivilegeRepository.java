package com.technicalchallenge.repository;

import com.technicalchallenge.model.Privilege;
import com.technicalchallenge.model.UserPrivilege;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPrivilegeRepository extends JpaRepository<UserPrivilege, Long> {
    @Query("""
        SELECT p FROM Privilege p
        JOIN UserPrivilege up ON up.privilegeId = p.id
        WHERE up.userId = :userId
    """)
    List<Privilege> findPrivilegesByUserId(@Param("userId") String userId);

    List<UserPrivilege> findByUserId(Long userID);
}
