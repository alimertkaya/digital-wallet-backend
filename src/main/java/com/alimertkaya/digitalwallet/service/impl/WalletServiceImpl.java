package com.alimertkaya.digitalwallet.service.impl;

import com.alimertkaya.digitalwallet.dto.CreateWalletRequest;
import com.alimertkaya.digitalwallet.dto.WalletResponse;
import com.alimertkaya.digitalwallet.entity.User;
import com.alimertkaya.digitalwallet.entity.Wallet;
import com.alimertkaya.digitalwallet.repository.WalletRepository;
import com.alimertkaya.digitalwallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;

    // mevcut user u alir
    public Mono<User> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(authentication -> {
                    Object principal = authentication.getPrincipal();
                    if (principal instanceof User) {
                        return Mono.just((User) principal);
                    }
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Kullanıcı oturumu bulunamadı."));
                });
    }

    @Override
    public Mono<WalletResponse> createWallet(CreateWalletRequest request) {
        return getCurrentUser().flatMap(user -> {
            Wallet newWallet = Wallet.builder()
                    .userId(user.getId())
                    .name(request.getName())
                    .currencyCode(request.getCurrencyCode().toUpperCase())
                    .balance(BigDecimal.ZERO)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            return walletRepository.save(newWallet)
                    .map(WalletResponse::fromEntity);
        });
    }

    @Override
    public Flux<WalletResponse> getCurrentUserWallets() {
        return getCurrentUser()
                .flatMapMany(user -> walletRepository.findByUserId(user.getId()))
                .map(WalletResponse::fromEntity);
    }

    @Override
    public Mono<WalletResponse> getWalletById(Long walletId) {
        return getCurrentUser()
                .flatMap(user -> walletRepository.findByIdAndUserId(walletId, user.getId()))
                .map(WalletResponse::fromEntity)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Cüzdan bulunamadı veya bu cüzdana erişim yetkiniz yok.")));
    }
}