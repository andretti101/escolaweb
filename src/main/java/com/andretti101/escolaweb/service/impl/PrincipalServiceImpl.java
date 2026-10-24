package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.exception.BusinessException;
import com.andretti101.escolaweb.model.entity.Principal;
import com.andretti101.escolaweb.repository.AnnouncementRepository;
import com.andretti101.escolaweb.repository.ChatMessageRepository;
import com.andretti101.escolaweb.repository.PrincipalRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.PrincipalService;
import com.andretti101.escolaweb.service.RefreshTokenService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Regras de negócio para cadastro, alteração e controle de acesso de diretores escolares
@Service
@RequiredArgsConstructor
public class PrincipalServiceImpl implements PrincipalService {

    private final PrincipalRepository principalRepository;
    private final UserRepository userRepository;
    private final AnnouncementRepository announcementRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.context.MessageSource messageSource;

    // Cadastra um novo diretor aplicando criptografia BCrypt na senha e ativando a conta
    @Override
    @Transactional
    public Principal create(Principal principal) {
        if (userRepository.existsByEmail(principal.getEmail())) {
            throw new IllegalStateException(messageSource.getMessage("error.email.in.use", null, org.springframework.context.i18n.LocaleContextHolder.getLocale()));
        }
        principal.setPassword(passwordEncoder.encode(principal.getPassword()));
        principal.setActive(true);
        return principalRepository.save(principal);
    }

    @Override
    @Transactional
    public Principal update(Integer id, Principal incoming) {
        Principal existing = findPrincipalOrThrow(id);

        if (!existing.getEmail().equals(incoming.getEmail())
                && userRepository.existsByEmailAndIdNot(incoming.getEmail(), id)) {
            throw new IllegalStateException(messageSource.getMessage("error.email.in.use", null, org.springframework.context.i18n.LocaleContextHolder.getLocale()));
        }

        existing.setName(incoming.getName());
        existing.setEmail(incoming.getEmail());

        String incomingPassword = incoming.getPassword();
        if (incomingPassword != null && !incomingPassword.isBlank() && !incomingPassword.equals("SenhaTemporaria123")) {
            existing.setPassword(passwordEncoder.encode(incomingPassword));
        }

        return principalRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Principal principal = findPrincipalOrThrow(id);
        if (announcementRepository.existsByAuthor_Id(id) || chatMessageRepository.existsBySender_Id(id)) {
            throw new BusinessException("Não é possível excluir o usuário pois existem comunicados ou mensagens vinculadas. Recomenda-se desativar o cadastro.");
        }
        refreshTokenService.revokeAllByUser(principal);
        principalRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Principal findById(Integer id) {
        return findPrincipalOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Principal> findAll() {
        return principalRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Principal> findAll(org.springframework.data.domain.Pageable pageable) {
        return principalRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Principal> findAllActive() {
        return principalRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Principal> findAllActive(org.springframework.data.domain.Pageable pageable) {
        return principalRepository.findByActiveTrue(pageable);
    }

    @Override
    @Transactional
    public Principal activate(Integer id) {
        Principal principal = findPrincipalOrThrow(id);
        principal.setActive(true);
        return principal;
    }

    @Override
    @Transactional
    public Principal deactivate(Integer id) {
        Principal principal = findPrincipalOrThrow(id);
        principal.setActive(false);
        refreshTokenService.revokeAllByUser(principal);
        return principal;
    }

    private Principal findPrincipalOrThrow(Integer id) {
        return principalRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(messageSource.getMessage("error.principal.not.found", new Object[]{id}, org.springframework.context.i18n.LocaleContextHolder.getLocale())));
    }
}
