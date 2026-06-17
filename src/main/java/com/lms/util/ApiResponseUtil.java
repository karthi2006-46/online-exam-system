package com.lms.util;

import com.lms.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ApiResponseUtil {

    public static ResponseEntity<ApiResponse> success(String message, Object data) {
        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public static ResponseEntity<ApiResponse> success(String message) {
        return new ResponseEntity<>(new ApiResponse(true, message), HttpStatus.OK);
    }

    public static ResponseEntity<ApiResponse> created(String message, Object data) {
        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    public static ResponseEntity<ApiResponse> error(String message) {
        return new ResponseEntity<>(new ApiResponse(false, message), HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<ApiResponse> error(String message, HttpStatus status) {
        return new ResponseEntity<>(new ApiResponse(false, message), status);
    }
}
