package tz.elmkusoma.administration.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;
    
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;
    
    @Size(max = 255, message = "Phone must not exceed 255 characters")
    private String phone;
    
    private tz.elmkusoma.shared.domain.User.Role role;
    
    private Boolean isActive;
    
    private Boolean isEmailVerified;
    
    private Boolean isPhoneVerified;
}