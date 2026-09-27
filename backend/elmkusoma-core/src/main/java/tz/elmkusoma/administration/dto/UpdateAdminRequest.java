package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import tz.elmkusoma.shared.domain.User;

@Data
public class UpdateAdminRequest {
    
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    private String phone;
    
    private User.Role role;
    
    private Boolean isActive;
}