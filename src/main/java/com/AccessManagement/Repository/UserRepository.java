package com.AccessManagement.Repository;

import com.AccessManagement.Model.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users,Integer> {


    Users  findByIsDeletedAndId(boolean b,Integer userId);

    Users findByUsername(String username);

    Users findByOtp(Integer otp);

    List<Users> findByIsDeletedAndOrganizationId(boolean b, Integer id);
    Page<Users> findByIsDeletedAndOrganizationId(boolean b, Integer id, Pageable pageable);

    List<Users> findByIsDeletedAndIsActiveAndOrganizationId(boolean b, boolean b1, Integer id);




    Users findByIsDeletedAndIsActiveAndUsername(boolean b, boolean b1, String username);

    List<Users> findByIsDeletedAndIsActive(boolean b, boolean b1);

    Page<Users> findByIsDeletedAndSubOrganizationId(boolean b, Integer orgId, Pageable pageable);
    List<Users> findByIsDeletedAndSubOrganizationId(boolean b, Integer orgId);

    Page<Users> findByIsDeletedAndSubOrganizationIdAndIdNot(boolean b, Integer subOrgId, Integer userId, Pageable pageable);

    Users findByIsDeletedAndSubOrganizationIdAndId(boolean b, Integer subOrgId, Integer userId);

    Users findByIsLicenseAssignAndIsDeletedAndSubOrganizationIdAndId(boolean b, boolean b1, Integer subOrgId, Integer userId);

    boolean existsByIsDeletedAndUserId(boolean b, String generatedAcceptedRejectedCode);

    ;
}
