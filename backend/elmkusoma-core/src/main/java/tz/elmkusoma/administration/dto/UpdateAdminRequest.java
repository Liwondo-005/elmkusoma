package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import tz.elmkusoma.shared.domain.User;
import java.util.UUID;

@Data
public class UpdateAdminRequest {
    
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    private String phone;
    
    private User.Role role;
    
    private Boolean isActive;
    
    private UUID institutionId;
    
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}