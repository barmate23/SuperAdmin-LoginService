package com.AccessManagement.Service;

import com.AccessManagement.EmailModule.EmailSender;
import com.AccessManagement.Model.*;
import com.AccessManagement.Repository.*;
import com.AccessManagement.Request.UserRequest;
import com.AccessManagement.Response.BaseResponse;
import com.AccessManagement.Response.UserResponse;
import com.AccessManagement.Utils.Const;
import com.AccessManagement.Utils.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class UserServiceImpl implements UserService{
    @Autowired
    UserRepository userRepository;

    @Autowired
    OrganizationRepository organizationRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    LoginUser loginUser;

    @Autowired
    EmailSender emailSender;

    @Autowired
    ValidationService validationService;

    @Autowired
    SubModuleRepository subModuleRepository;
    @Autowired
    UserLicenseKeyRepository userLicenseKeyRepository;
    @Autowired
    SupplierRepository supplierRepository;
    @Autowired
    DeviceRepository deviceRepository;
    @Autowired
    UserDeviceLicenseMapperRepository userDeviceLicenseMapperRepository;

    @Autowired
    ModulesRepository modulesRepository;

    @Autowired
    ReasonRepository reasonRepository;


    @Override
    public BaseResponse<Users> createUsers(UserRequest userRequest){
        long startTime = System.currentTimeMillis();
        log.info(loginUser.getLogId()+" CREATE USERS METHOD START ");
        BaseResponse baseResponse=new BaseResponse<>();
        try {
            Users users = new Users();
            if (loginUser.getOrgId() != null && loginUser.getSubOrgId()!=null){
                users.setOrganization(organizationRepository.findByIsDeletedAndId(false,loginUser.getOrgId()));
                users.setSubOrganization(organizationRepository.findByIsDeletedAndId(false,loginUser.getSubOrgId()));
            }
            if(!validationService.isUsernameValidation(userRequest.getUserName(),userRequest.getFirstName(),userRequest.getLastName())) {
                users.setUsername(userRequest.getUserName());
                users.setFirstName(userRequest.getFirstName());
                users.setLastName(userRequest.getLastName());
            }else {
                baseResponse.setData(new ArrayList<>());
                baseResponse.setLogId(loginUser.getLogId());
                baseResponse.setStatus(Const.STATUS_500);
                baseResponse.setMessage("THIS USER NAME ALREADY USED PLEASE RE-ENTER");
                log.info(loginUser.getLogId()+" THIS USER NAME ALREADY USED PLEASE RE-ENTER ");
                return baseResponse;
            }
            Optional<ModuleUserLicenceKey> userLicenseKeys=null;
            if(userRequest.getUserLicenseKeyId()!=null){
                userLicenseKeys= Optional.ofNullable(userLicenseKeyRepository.findByIsDeletedAndIsUsedAndId(false, false, userRequest.getUserLicenseKeyId()));
                if(userLicenseKeys.isPresent()) {
                    users.setModuleUserLicenceKey(userLicenseKeys.get());
                    userLicenseKeys.get().setIsUsed(true);
                }else {
                    baseResponse.setData(new ArrayList<>());
                    baseResponse.setLogId(loginUser.getLogId());
                    baseResponse.setStatus(Const.STATUS_500);
                    baseResponse.setMessage(" LICENSE KEY IS ALREADY USED! PLEASE SELECT ANOTHER KEY");
                    log.info(loginUser.getLogId()+" THIS LICENSE KEY IS NOT VALID! PLEASE SELECT ANOTHER KEY ");
                    return baseResponse;
                }
            }
            Optional<Supplier>supplier= Optional.ofNullable(supplierRepository.findByIsDeletedAndId(false, userRequest.getSupplierId()));
            supplier.ifPresent(users::setSupplier);
            users.setIsLicenseAssign(true);
            users.setIsDefaultUser(false);
            users.setUserType(userRequest.getUserType());
            users.setUsers(userRequest.getUsers());
            users.setIsFirstLogin(true);
            String pwd=validationService.generateRandomPassword();
            String password=passwordEncoder.encode(pwd);
            users.setPassword(password);
            if(validationService.isValidEmail(userRequest.getEmailID())) {
                if(!validationService.duplicateEmailValidation(userRequest.getEmailID())) {
                    users.setEmailId(userRequest.getEmailID());
                }else {
                    baseResponse.setData(new ArrayList<>());
                    baseResponse.setLogId(loginUser.getLogId());
                    baseResponse.setStatus(Const.STATUS_500);
                    baseResponse.setMessage("THIS EMAIL ID ALREADY USED PLEASE RE-ENTER");
                    log.info(loginUser.getLogId()+" THIS USER NAME ALREADY USED PLEASE RE-ENTER ");
                    return baseResponse;
                }
            }else {
                baseResponse.setData(new ArrayList<>());
                baseResponse.setLogId(loginUser.getLogId());
                baseResponse.setStatus(Const.STATUS_500);
                baseResponse.setMessage("EMAIL ID IS NOT VALID PLEASE RE-ENTER");
                log.info(loginUser.getLogId()+" EMAIL ID IS NOT VALID PLEASE RE-ENTER ");
                return baseResponse;
            }
            users.setStartDate(userRequest.getStartDate());
            String generatedAcceptedRejectedCode;
            do {
                generatedAcceptedRejectedCode = generateAcceptedRejectedId();
            } while (userRepository.existsByIsDeletedAndUserId(false,generatedAcceptedRejectedCode));
            users.setUserId(generatedAcceptedRejectedCode);
            users.setEndDate(userRequest.getEndDate());
            users.setDateOfBirth(userRequest.getDateOfBirth());
            users.setDepartment(userRequest.getDepartment());
            users.setDesignation(userRequest.getDesignation());
            users.setMobileNo(userRequest.getMobileNo());
            users.setIsActive(true);
            users.setFirstPassword(password);
            users.setPwdExpireDate(System.currentTimeMillis() + Const.PWD_EXPIRY_DURATION);
            users.setIsEmailVerified(userRequest.getIsEmailVerified());
            users.setIsDeleted(false);
            users.setCreatedOn(new Date());
            users.setCreatedBy(loginUser.getUserId());
            users.setModifiedOn(new Date());
            users.setModifiedBy(loginUser.getUserId());
            this.userRepository.save(users);
            if(userRequest.getAdditionalDeviceLicense()!=null && userRequest.getAdditionalDeviceLicense().size()!=0){
                List<ModuleUserLicenceKey> moduleUserLicenceKey=userLicenseKeyRepository.findByIsDeletedAndIdIn(false,userRequest.getAdditionalDeviceLicense());
                for (ModuleUserLicenceKey moduleUserLicenceKe: moduleUserLicenceKey) {
                    UserDeviceLicenseMapper userDeviceLicenseMapper=new UserDeviceLicenseMapper();
                    userDeviceLicenseMapper.setUser(users);
                    userDeviceLicenseMapper.setAdditionalUserDeviceLicense(moduleUserLicenceKe);
                    userDeviceLicenseMapper.setModuleUserLicenceKey(users.getModuleUserLicenceKey());
                    userDeviceLicenseMapper.setIsDeleted(false);
                    userDeviceLicenseMapper.setCreatedOn(new Date());
                    userDeviceLicenseMapper.setCreatedBy(loginUser.getUserId());
                    userDeviceLicenseMapper.setModifiedBy(loginUser.getUserId());
                    userDeviceLicenseMapper.setModifiedOn(new Date());
                    userDeviceLicenseMapperRepository.save(userDeviceLicenseMapper);
                }
            }
            userLicenseKeyRepository.save(userLicenseKeys.get());
            List<Users> usersList=new ArrayList<>();
            usersList.add(users);
            baseResponse.setCode(1);
            baseResponse.setData(usersList);
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(200);
            baseResponse.setMessage("USER CREATE SUCCESSFULLY");
//            emailSender.sendMail("Create User","Your Registration has Successfully username is: "+users.getUsername()+" password is: "+pwd ,users.getEmailId(),users.getOrganization().getId());
            log.info(loginUser.getLogId()+" USER CREATE SUCCESSFULLY ");
        }catch (Exception e){
            baseResponse.setCode(Const.CODE_0);
            baseResponse.setData(new ArrayList<>());
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(Const.STATUS_500);
            baseResponse.setMessage("FAILED TO CREATE USER");
            long endTime = System.currentTimeMillis();
            log.error(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" ERROR OCCURS AT CREATE USER EXECUTED TIME ").append(endTime-startTime)),e);
        }
        long endTime = System.currentTimeMillis();
        log.info(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" CREATE USER METHOD  EXECUTED TIME ").append(endTime-startTime)));
        return baseResponse;
    }
    @Override
    public BaseResponse<Users> updateUsers(UserRequest userRequest, Integer userId){
        long startTime = System.currentTimeMillis();
        log.info(loginUser.getLogId()+"UPDATE USERS METHOD START");
        BaseResponse baseResponse=new BaseResponse<>();
        try {
             Users users = this.userRepository.findByIsDeletedAndId(false,userId);
            if (loginUser.getOrgId() != null && loginUser.getSubOrgId()!=null){
                users.setOrganization(organizationRepository.findByIsDeletedAndId(false,loginUser.getOrgId()));
                users.setSubOrganization(organizationRepository.findByIsDeletedAndId(false,loginUser.getSubOrgId()));
            }
            if(users.getUsername().equalsIgnoreCase(userRequest.getUserName()) && users.getFirstName().equalsIgnoreCase(userRequest.getFirstName()) && users.getLastName().equalsIgnoreCase(userRequest.getLastName())) {
                users.setUsername(userRequest.getUserName());
            }else {
                if(!validationService.isUsernameValidation(userRequest.getUserName(),userRequest.getFirstName(),userRequest.getLastName())) {
                    users.setUsername(userRequest.getUserName());
                    users.setFirstName(userRequest.getFirstName());
                    users.setLastName(userRequest.getLastName());
                }else {
                    baseResponse.setData(new ArrayList<>());
                    baseResponse.setLogId(loginUser.getLogId());
                    baseResponse.setStatus(Const.STATUS_200);
                    baseResponse.setMessage("THIS USER NAME ALREADY USED PLEASE RE-ENTER");
                    log.info(loginUser.getLogId()+" THIS USER NAME ALREADY USED PLEASE RE-ENTER ");
                    return baseResponse;
                }
            }
            users.setUserType(userRequest.getUserType());
            users.setStartDate(userRequest.getStartDate());
            users.setUsers(userRequest.getUsers());
            users.setEndDate(userRequest.getEndDate());
            users.setDateOfBirth(userRequest.getDateOfBirth());
            users.setDepartment(userRequest.getDepartment());
            users.setDesignation(userRequest.getDesignation());
            if(userRequest.getUserLicenseKeyId()!=null && userRequest.getIsLicenseAssign()){
                ModuleUserLicenceKey userLicenseKeys=userLicenseKeyRepository.findByIsDeletedAndIdAndSubOrganizationId(false,userRequest.getUserLicenseKeyId(),loginUser.getSubOrgId());
                if(users.getModuleUserLicenceKey().getUserLicenseKey().equals(userLicenseKeys.getUserLicenseKey())){
                    users.setModuleUserLicenceKey(userLicenseKeys);
                    userLicenseKeys.setIsUsed(true);
                    userLicenseKeyRepository.save(userLicenseKeys);
                }else {
                    ModuleUserLicenceKey userLicenseKey=userLicenseKeyRepository.findByIsDeletedAndIsUsedAndId(false,false,userRequest.getUserLicenseKeyId());
                    userLicenseKey.setIsUsed(false);
                    userLicenseKeyRepository.save(userLicenseKey);
                    users.setModuleUserLicenceKey(userLicenseKeys);
                    userLicenseKeys.setIsUsed(true);
                    userLicenseKeyRepository.save(userLicenseKeys);
                }
            }else {
                ModuleUserLicenceKey userLicenseKey=userLicenseKeyRepository.findByIsDeletedAndIsUsedAndId(false,true,userRequest.getUserLicenseKeyId());
                userLicenseKey.setIsUsed(false);
                userLicenseKeyRepository.save(userLicenseKey);
                users.setIsLicenseAssign(false);
                users.setModuleUserLicenceKey(null);
                users.setReason(reasonRepository.findByIsDeletedAndSubOrganizationIdAndId(false,loginUser.getSubOrgId(),userRequest.getReasonId()));
            }
            users.setIsFirstLogin(false);
            users.setIsActive(true);
            users.setLastName(userRequest.getLastName());  if(validationService.isValidEmail(userRequest.getEmailID())) {
                users.setEmailId(userRequest.getEmailID());
            }else {
                baseResponse.setData(new ArrayList<>());
                baseResponse.setLogId(loginUser.getLogId());
                baseResponse.setStatus(200);
                baseResponse.setMessage("EMAIL ID IS NOT VALID PLEASE RE-ENTER");
                log.info(loginUser.getLogId()+" EMAIL ID IS NOT VALID PLEASE RE-ENTER ");
                return baseResponse;
            }
            Optional<Supplier>supplier= Optional.ofNullable(supplierRepository.findByIsDeletedAndId(false, userRequest.getSupplierId()));
            supplier.ifPresent(users::setSupplier);
            users.setMobileNo(userRequest.getMobileNo());
            users.setIsEmailVerified(userRequest.getIsEmailVerified());
            users.setIsDeleted(false);
            users.setModifiedBy(loginUser.getUserId());
            users.setModifiedOn(new Date());
            this.userRepository.save(users);
            if(userRequest.getIsLicenseAssign() && userRequest.getAdditionalDeviceLicense()!=null && userRequest.getAdditionalDeviceLicense().size()!=0){
                for (Integer id : userRequest.getAdditionalDeviceLicense()){
                    Optional<UserDeviceLicenseMapper> userDeviceLicenseMapper=userDeviceLicenseMapperRepository.findByIsDeletedAndId(false,id);
                         if(userDeviceLicenseMapper.isPresent()){
                             userDeviceLicenseMapper.get().setUser(users);
                             userDeviceLicenseMapper.get().setModuleUserLicenceKey(users.getModuleUserLicenceKey());
                             userDeviceLicenseMapper.get().setIsDeleted(false);
                    userDeviceLicenseMapper.get().setCreatedOn(new Date());
                    userDeviceLicenseMapper.get().setCreatedBy(loginUser.getUserId());
                    userDeviceLicenseMapper.get().setModifiedBy(loginUser.getUserId());
                    userDeviceLicenseMapper.get().setModifiedOn(new Date());
                    userDeviceLicenseMapperRepository.save(userDeviceLicenseMapper.get());
                         }else {
                             UserDeviceLicenseMapper userDeviceLicenseMappers=new UserDeviceLicenseMapper();
                             userDeviceLicenseMappers.setUser(users);
                             userDeviceLicenseMappers.setAdditionalUserDeviceLicense(userLicenseKeyRepository.findByIsDeletedAndId(false,id));
                             userDeviceLicenseMappers.setModuleUserLicenceKey(users.getModuleUserLicenceKey());
                             userDeviceLicenseMappers.setIsDeleted(false);
                             userDeviceLicenseMappers.setCreatedOn(new Date());
                             userDeviceLicenseMappers.setCreatedBy(loginUser.getUserId());
                             userDeviceLicenseMappers.setModifiedBy(loginUser.getUserId());
                             userDeviceLicenseMappers.setModifiedOn(new Date());
                             userDeviceLicenseMapperRepository.save(userDeviceLicenseMappers);
                         }
                }
            }else {
                userDeviceLicenseMapperRepository.deleteAllByIdIn(userRequest.getAdditionalDeviceLicense());
                log.info(loginUser.getLogId()+" LICENSE  ARE UN ASSIGN ");
            }
            List<Users> usersList=new ArrayList<>();
            usersList.add(users);
            baseResponse.setCode(1);
            baseResponse.setData(usersList);
            baseResponse.setLogId(null);
            baseResponse.setStatus(Const.STATUS_200);
            baseResponse.setMessage("USER UPDATE SUCCESSFULLY");
            log.info(loginUser.getLogId()+" USER UPDATE SUCCESSFULLY ");
        }catch (Exception e){
            baseResponse.setCode(0);
            baseResponse.setData(new ArrayList<>());
            baseResponse.setLogId(null);
            baseResponse.setStatus(Const.STATUS_500);
            baseResponse.setMessage(" FAILED TO UPDATE USER ");
            long endTime = System.currentTimeMillis();
            log.error(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" ERROR OCCURS AT UPDATING USER EXECUTED TIME ").append(endTime-startTime)),e);
        }
        long endTime = System.currentTimeMillis();
        log.info(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" UPDATE USER METHOD  EXECUTED TIME ").append(endTime-startTime)));
        return baseResponse;
    }
    @Override
    public BaseResponse<Users> getUserById(Integer userId){
        long startTime = System.currentTimeMillis();
        log.info(loginUser.getLogId()+" GET USERS BY ID METHOD START");
        BaseResponse baseResponse=new BaseResponse<>();
        try{

            Users users = this.userRepository.findByIsDeletedAndId(false,userId);
            List<ModuleUserLicenceKey>moduleUserLicenceKeyList=new ArrayList<>();
           List<UserDeviceLicenseMapper> userDeviceLicenseMappers=userDeviceLicenseMapperRepository.findByIsDeletedAndUserId(false,userId);
            for (UserDeviceLicenseMapper userDeviceLicenseMapper:userDeviceLicenseMappers) {
               moduleUserLicenceKeyList.add(userDeviceLicenseMapper.getAdditionalUserDeviceLicense());
            }
            users.setAdditionalUserDeviceLicense(moduleUserLicenceKeyList);
            List<Users> usersList=new ArrayList<>();
            usersList.add(users);
            baseResponse.setCode(1);
            baseResponse.setData(usersList);
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(200);
            baseResponse.setMessage(" SUCCESSFULLY FETCHED USER ");
            log.info(loginUser.getLogId()+" SUCCESSFULLY FETCHED USER ");
        }catch(Exception e){
            baseResponse.setCode(0);
            baseResponse.setData(new ArrayList<>());
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(500);
            baseResponse.setMessage("FAILED TO FETCHED USER");
            long endTime = System.currentTimeMillis();
            log.error(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" ERROR OCCURS AT GETTING USER EXECUTED TIME ").append(endTime-startTime)),e);
        }
        long endTime = System.currentTimeMillis();
        log.info(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" GET USER BY ID METHOD  EXECUTED TIME ").append(endTime-startTime)));
        return baseResponse;
    }
    @Override
    public BaseResponse<Users> getAllUser(){
        long startTime = System.currentTimeMillis();
        log.info(loginUser.getLogId()+" GET ALL USERS METHOD START");
        BaseResponse baseResponse=new BaseResponse<>();
        try{
            List<Users>  users = this.userRepository.findByIsDeletedAndIsActive(false,true);
            baseResponse.setCode(1);
            baseResponse.setData(users);
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(200);
            baseResponse.setMessage("SUCCESSFULLY FETCHED USER LIST");
            log.info(loginUser.getLogId()+" SUCCESSFULLY FETCHED USER LIST ");
        }catch(Exception e){
            baseResponse.setCode(0);
            baseResponse.setData(new ArrayList<>());
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(500);
            baseResponse.setMessage("FAILED TO  FETCHED USER LISTt");
            long endTime = System.currentTimeMillis();
            log.error(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" ERROR OCCURS AT GETTING FETCHED LIST OF USER EXECUTED TIME ").append(endTime-startTime)),e);
        }
        long endTime = System.currentTimeMillis();
        log.info(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" GET ALL USER METHOD  EXECUTED TIME ").append(endTime-startTime)));
        return baseResponse;
    }

    @Override
    public BaseResponse<Users> getAllUserWithPagination(Integer pageNo, Integer pageSize){
        long startTime = System.currentTimeMillis();
        log.info(loginUser.getLogId()+" GET ALL USERS METHOD START");
        BaseResponse baseResponse=new BaseResponse<>();
        final Pageable pageable = (Pageable) PageRequest.of((int) pageNo, (int) pageSize);
        Page<Users> pageResult;
        try{
            if(pageSize!=null && pageSize!=0) {
                pageResult = this.userRepository.findByIsDeletedAndSubOrganizationIdAndIdNot(false,  loginUser.getSubOrgId(),loginUser.getUserId(), pageable);
                UserResponse userResponse = new UserResponse();
                userResponse.setUsersList(pageResult.getContent());
                userResponse.setPageCount(pageResult.getTotalPages());
                userResponse.setRecordCount(pageResult.getTotalElements());
                List<UserResponse> userResponses = new ArrayList<>();
                userResponses.add(userResponse);
                baseResponse.setCode(1);
                baseResponse.setData(userResponses);
                baseResponse.setLogId(loginUser.getLogId());
                baseResponse.setStatus(200);
                baseResponse.setMessage("SUCCESSFULLY FETCHED USER LIST");
                log.info(loginUser.getLogId() + " SUCCESSFULLY FETCHED USER LIST ");
            }else {
                baseResponse.setCode(Const.CODE_0);
                baseResponse.setData(new ArrayList());
                baseResponse.setLogId(loginUser.getLogId());
                baseResponse.setStatus(Const.STATUS_500);
                baseResponse.setMessage("PAGE SIZE MUST NOT BE LESS THAN ONE");
                log.info(loginUser.getLogId() + " PAGE SIZE MUST NOT BE LESS THAN ONE ");
            }
        }catch(Exception e){
            baseResponse.setCode(0);
            baseResponse.setData(new ArrayList<>());
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(500);
            baseResponse.setMessage("FAILED TO  FETCHED USER LISTt");
            long endTime = System.currentTimeMillis();
            log.error(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" ERROR OCCURS AT GETTING FETCHED LIST OF USER EXECUTED TIME ").append(endTime-startTime)),e);
        }
        long endTime = System.currentTimeMillis();
        log.info(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" GET ALL USER METHOD  EXECUTED TIME ").append(endTime-startTime)));
        return baseResponse;
    }

    @Override
    public BaseResponse<Users> deleteUserById(Integer userId){
        long startTime = System.currentTimeMillis();
        log.info(loginUser.getLogId()+" DELETE USERS METHOD START");
        BaseResponse baseResponse=new BaseResponse<>();
        try{
            Users users = this.userRepository.findByIsLicenseAssignAndIsDeletedAndSubOrganizationIdAndId(false,false,loginUser.getSubOrgId(),userId);
            if(users!=null){
                users.setIsDeleted(true);
                userRepository.save(users);
            }else{
                baseResponse.setCode(Const.CODE_0);
                baseResponse.setData(new ArrayList<>());
                baseResponse.setLogId(loginUser.getLogId());
                baseResponse.setStatus(Const.STATUS_500);
                baseResponse.setMessage("PLEASE UN ASSIGN THE LICENSE THEN DELETE USER");
                log.info(loginUser.getLogId()+" SUCCESSFULLY DELETE USER ");
                return baseResponse;
            }
            List<Users> usersList=new ArrayList<>();
            usersList.add(users);
            baseResponse.setCode(Const.CODE_0);
            baseResponse.setData(usersList);
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(Const.STATUS_500);
            baseResponse.setMessage(" SUCCESSFULLY DELETE USER ");
            log.info(loginUser.getLogId()+" SUCCESSFULLY DELETE USER ");

        }catch(Exception e){
            baseResponse.setCode(Const.CODE_0);
            baseResponse.setData(new ArrayList<>());
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(500);
            baseResponse.setMessage("FAILED TO DELETE USER ");
            long endTime = System.currentTimeMillis();
            log.error(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" ERROR OCCURS AT DELETED USER EXECUTED TIME ").append(endTime-startTime)),e);
        }
        long endTime = System.currentTimeMillis();
        log.info(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" DELETE USER METHOD  EXECUTED TIME ").append(endTime-startTime)));
        return baseResponse;
    }

    @Override
    public BaseResponse<Users> activeUserById(Integer userId,Boolean status){
        long startTime = System.currentTimeMillis();
        log.info(loginUser.getLogId()+" USER ACTIVE AND DEACTIVATE METHOD START");
        BaseResponse baseResponse=new BaseResponse<>();
        try{
            Optional<Users> users = Optional.ofNullable(this.userRepository.findByIsDeletedAndId(false, userId));
            if(status) {
                users.get().setIsActive(status);
                userRepository.save(users.get());
                List<Users> usersList = new ArrayList<>();
                usersList.add(users.get());
                baseResponse.setCode(Const.CODE_1);
                baseResponse.setData(usersList);
                baseResponse.setLogId(loginUser.getLogId());
                baseResponse.setStatus(Const.STATUS_200);
                baseResponse.setMessage(" USER ACTIVATED SUCCESSFULLY ");
                log.info(loginUser.getLogId() + " USER ACTIVATED SUCCESSFULLY ");
            }else {
                users.get().setIsActive(status);
                userRepository.save(users.get());
                List<Users> usersList = new ArrayList<>();
                usersList.add(users.get());
                baseResponse.setCode(Const.CODE_0);
                baseResponse.setData(usersList);
                baseResponse.setLogId(loginUser.getLogId());
                baseResponse.setStatus(Const.STATUS_200);
                baseResponse.setMessage(" USER DEACTIVATED SUCCESSFULLY ");
                log.info(loginUser.getLogId() + " USER DEACTIVATED SUCCESSFULLY ");
            }
        }catch(Exception e){
            baseResponse.setCode(0);
            baseResponse.setData(new ArrayList<>());
            baseResponse.setLogId(null);
            baseResponse.setStatus(500);
            baseResponse.setMessage("FAILED TO DELETE USER ");
            long endTime = System.currentTimeMillis();
            log.error(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" ERROR OCCURS AT DEACTIVATED & ACTIVATED USER EXECUTED TIME ").append(endTime-startTime)),e);
        }
        long endTime = System.currentTimeMillis();
        log.info(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" DEACTIVATED & ACTIVATED USER METHOD  EXECUTED TIME ").append(endTime-startTime)));
        return baseResponse;
    }
    @Override
    public BaseResponse getModule() {
        long startTime = System.currentTimeMillis();
        log.info(loginUser.getLogId() + " GET USERS BY ID METHOD START");
        BaseResponse baseResponse = new BaseResponse<>();
        try {
            List<Modules> modulesList = modulesRepository.findByIsDeleted(false);
            baseResponse.setCode(1);
            baseResponse.setData(modulesList);
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(200);
            baseResponse.setMessage(" SUCCESSFULLY FETCHED USER ");
            log.info(loginUser.getLogId() + " SUCCESSFULLY FETCHED USER ");
        } catch (Exception e) {
            baseResponse.setCode(0);
            baseResponse.setData(new ArrayList<>());
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(500);
            baseResponse.setMessage("FAILED TO FETCHED USER");
            long endTime = System.currentTimeMillis();
            log.error(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" ERROR OCCURS AT GETTING USER EXECUTED TIME ").append(endTime - startTime)), e);
        }
        long endTime = System.currentTimeMillis();
        log.info(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" GET USER BY ID METHOD EXECUTED TIME ").append(endTime - startTime)));
        return baseResponse;
    }
    @Override
    public BaseResponse getSubModule(Integer moduleId) {
        long startTime = System.currentTimeMillis();
        log.info(loginUser.getLogId() + " GET USERS BY ID METHOD START");
        BaseResponse baseResponse = new BaseResponse<>();
        try {
            List<SubModules> subModulesList = subModuleRepository.findByIsDeletedAndModuleId(false, moduleId);
            baseResponse.setCode(1);
            baseResponse.setData(subModulesList);
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(200);
            baseResponse.setMessage(" SUCCESSFULLY FETCHED USER ");
            log.info(loginUser.getLogId() + " SUCCESSFULLY FETCHED USER ");
        } catch (Exception e) {
            baseResponse.setCode(0);
            baseResponse.setData(new ArrayList<>());
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(500);
            baseResponse.setMessage("FAILED TO FETCHED USER");
            long endTime = System.currentTimeMillis();
            log.error(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" ERROR OCCURS AT GETTING USER EXECUTED TIME ").append(endTime - startTime)), e);
        }
        long endTime = System.currentTimeMillis();
        log.info(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" GET USER BY ID METHOD EXECUTED TIME ").append(endTime - startTime)));
        return baseResponse;
    }
    @Override
    public BaseResponse getUserLicense(Integer subModuleId) {
        long startTime = System.currentTimeMillis();
        log.info(loginUser.getLogId() + " GET USERS BY ID METHOD START");
        BaseResponse baseResponse = new BaseResponse<>();
        try {
            List<ModuleUserLicenceKey> moduleUserLicenceKeyList =userLicenseKeyRepository.findByIsDeletedAndIsUsedAndLicenceLineSubModuleIdAndLicenceLinePartNumberSubModuleMapperLicenseCategoryAndSubOrganizationId(false, false, subModuleId, 1, loginUser.getSubOrgId());
            baseResponse.setCode(1);
            baseResponse.setData(moduleUserLicenceKeyList);
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(200);
            baseResponse.setMessage(" SUCCESSFULLY FETCHED USER ");
            log.info(loginUser.getLogId() + " SUCCESSFULLY FETCHED USER ");
        } catch (Exception e) {
            baseResponse.setCode(0);
            baseResponse.setData(new ArrayList<>());
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(500);
            baseResponse.setMessage("FAILED TO FETCHED USER");
            long endTime = System.currentTimeMillis();
            log.error(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" ERROR OCCURS AT GETTING USER EXECUTED TIME ").append(endTime - startTime)), e);
        }
        long endTime = System.currentTimeMillis();
        log.info(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" GET USER BY ID METHOD EXECUTED TIME ").append(endTime - startTime)));
        return baseResponse;
    }
    @Override
    public BaseResponse getDeviceLicense(Integer subModuleId) {
        long startTime = System.currentTimeMillis();
        log.info(loginUser.getLogId() + " GET USERS BY ID METHOD START");
        BaseResponse baseResponse = new BaseResponse<>();
        try {
            List<ModuleUserLicenceKey> moduleUserLicenceKeyList = userLicenseKeyRepository.findByIsDeletedAndIsUsedAndLicenceLineSubModuleIdAndLicenceLinePartNumberSubModuleMapperLicenseCategoryAndSubOrganizationId(false, false, subModuleId, 2, loginUser.getSubOrgId());
            baseResponse.setCode(1);
            baseResponse.setData(moduleUserLicenceKeyList);
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(200);
            baseResponse.setMessage(" SUCCESSFULLY FETCHED USER ");
            log.info(loginUser.getLogId() + " SUCCESSFULLY FETCHED USER ");
        } catch (Exception e) {
            baseResponse.setCode(0);
            baseResponse.setData(new ArrayList<>());
            baseResponse.setLogId(loginUser.getLogId());
            baseResponse.setStatus(500);
            baseResponse.setMessage("FAILED TO FETCHED USER");
            long endTime = System.currentTimeMillis();
            log.error(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" ERROR OCCURS AT GETTING USER EXECUTED TIME ").append(endTime - startTime)), e);
        }
        long endTime = System.currentTimeMillis();
        log.info(String.valueOf(new StringBuilder(loginUser.getLogId()).append(" GET USER BY ID METHOD EXECUTED TIME ").append(endTime - startTime)));
        return baseResponse;
    }
    public String generateAcceptedRejectedId() {
        int acceptedRejectedNumber = Const.accepteRejectedCounter.getAndIncrement();
        int arNumber = (acceptedRejectedNumber - 1) / Const.MAX_ACCEPTED_REJECTED__PER_AR + 1;
        int acceptedRejectedWithinAr = (acceptedRejectedNumber - 1) % Const.MAX_ACCEPTED_REJECTED__PER_AR + 1;
        return Const.PREFIX_ACCEPTED_REJECTED + String.format("%02d", arNumber) + Const.DELIMITER_ACCEPTED_REJECTED_STAGING_AREA + acceptedRejectedWithinAr;
    }
}
