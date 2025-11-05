package com.alimertkaya.digitalwallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Kullanıcı adı boş bırakılamaz!")
    @Size(min = 3, max = 50, message = "Kullanıcı adı 3 ile 50 karakter arasında olmalıdır.")
    private String username;

    // response olarak donen JSON a password u eklemiyoruz
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Şifre boş bırakılamaz!")
    @Size(min = 8, message = "Şifre en az 8 karakter olmalıdır")
    private String password;

    @NotBlank(message = "E-posta alanı boş bırakılamaz!")
    @Email(message = "Geçerli bir e-posta adresi giriniz.")
    private String email;

    @NotBlank(message = "Telefon numarası alanı boş bırakılamaz!")
    private String phoneNumber;

    @NotBlank(message = "İsim boş bırakılamaz!")
    private String firstName;

    @NotBlank(message = "Soyisim boş bırakılamaz!")
    private String lastName;

    @Past(message = "Doğum tarihi geçmiş bir tarih olmalıdır!")
    private LocalDate birthDate;

    @NotBlank(message = "TCKN boş bırakılamaz!")
    @Size(min = 11, max = 11, message = "TCKN 11 haneli olmalıdır!")
    private String tckn;
}
