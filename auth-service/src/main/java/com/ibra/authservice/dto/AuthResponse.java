package com.ibra.authservice.dto;


import com.ibra.authservice.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private UserRole role;

    public AuthResponse(String token, Long id, String email, String firstName, String lastName, UserRole role) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

}
