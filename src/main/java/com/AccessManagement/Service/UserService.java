package com.AccessManagement.Service;

import com.AccessManagement.Model.Users;
import com.AccessManagement.Request.UserRequest;
import com.AccessManagement.Response.BaseResponse;


public interface UserService {
    BaseResponse<Users> createUsers(UserRequest userRequest);

    BaseResponse<Users> updateUsers(UserRequest userRequest, Integer userId);

    BaseResponse<Users> getUserById(Integer userId);

    BaseResponse<Users> getAllUser();

    BaseResponse<Users> getAllUserWithPagination(Integer pageNo, Integer pageSize);

    BaseResponse<Users> deleteUserById(Integer userId);


    BaseResponse<Users> activeUserById(Integer userId, Boolean status);

    BaseResponse getModule();

    BaseResponse getSubModule(Integer moduleId);

    BaseResponse getUserLicense(Integer subModuleId);

    BaseResponse getDeviceLicense(Integer subModuleId);
}
