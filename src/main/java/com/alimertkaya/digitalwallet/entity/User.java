package com.alimertkaya.digitalwallet.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User implements UserDetails {

    @Id
    private Long id;

    @Column("username")
    private String username;

    @Column("password")
    private String password;

    @Column("email")
    private String email;

    @Column("phone_number")
    private String phoneNumber;

    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("birth_date")
    private LocalDate birthDate;

    @Column("tckn")
    private String tckn;

    @Column("roles")
    private String roles;

    @Column("is_enabled")
    private boolean isEnabled;

    @Column("is_locked")
    private boolean isLocked;

    @Column("is_email_verified")
    private boolean isEmailVerified;

    @Column("is_phone_verified")
    private boolean isPhoneVerified;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.stream(roles.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // hesap süresi
    }

    @Override
    public boolean isAccountNonLocked() {
        return !isLocked; // hesap kilit durumu
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // parola süresi
    }

    @Override
    public boolean isEnabled() {
        return isEnabled; // hesap aktiflik durumu
    }
}
