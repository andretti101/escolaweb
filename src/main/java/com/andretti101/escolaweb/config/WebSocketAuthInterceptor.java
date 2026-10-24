package com.andretti101.escolaweb.config;

import com.andretti101.escolaweb.model.entity.User;
import com.andretti101.escolaweb.model.enums.UserRole;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.UserRepository;
import com.andretti101.escolaweb.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // 1. Conexao inicial: valida o token JWT Bearer e autentica a sessao WebSocket
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtTokenProvider.extractUsername(token);
                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (userDetails.isEnabled() && jwtTokenProvider.validateToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        accessor.setUser(authentication);
                    } else {
                        throw new AccessDeniedException("Token inválido ou usuário inativo.");
                    }
                } else {
                    throw new AccessDeniedException("Token inválido.");
                }
            }
        // 2. Inscricao em canais: garante que cada perfil so escute topicos autorizados
        } else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal principal = accessor.getUser();
            if (principal == null) {
                throw new IllegalArgumentException("Usuário não autenticado no WebSocket.");
            }
            String destination = accessor.getDestination();
            if (destination == null || destination.contains("*") || destination.contains("?")) {
                throw new AccessDeniedException("Destino de inscrição inválido.");
            }

            User user = userRepository.findByEmail(principal.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

            // Notificacoes pessoais: restrito ao proprio usuario ou a gestao escolar
            if (destination.startsWith("/topic/user/")) {
                String[] parts = destination.split("/");
                if (parts.length >= 4) {
                    try {
                        Integer targetUserId = Integer.parseInt(parts[3]);
                        if (!user.getId().equals(targetUserId) && user.getRole() != UserRole.PRINCIPAL && user.getRole() != UserRole.SECRETARY) {
                            throw new IllegalArgumentException("Acesso negado ao canal de notificações de outro usuário.");
                        }
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Destino inválido para notificações.");
                    }
                } else {
                    throw new IllegalArgumentException("Destino inválido para notificações.");
                }
            // Sala dos professores: alunos nao podem acessar
            } else if (destination.startsWith("/topic/teachers")) {
                if (user.getRole() == UserRole.STUDENT) {
                    throw new IllegalArgumentException("Acesso negado: Alunos não podem acessar o chat dos professores.");
                }
            // Chat da turma: restrito aos discentes com matricula ativa na sala
            } else if (destination.startsWith("/topic/classroom/")) {
                String roomIdStr = destination.substring("/topic/classroom/".length());
                Integer roomId;
                try {
                    roomId = Integer.parseInt(roomIdStr);
                } catch (NumberFormatException e) {
                    throw new AccessDeniedException("Destino inválido para chat de turma.");
                }

                // Direcao e secretaria possuem supervisao livre em qualquer turma
                if (user.getRole() == UserRole.SECRETARY || user.getRole() == UserRole.PRINCIPAL) {
                    return message;
                }

                // Se for aluno, verifica se esta ativamente matriculado na turma solicitada
                if (user.getRole() == UserRole.STUDENT) {
                    boolean enrolled = enrollmentRepository.existsByStudent_IdAndClassRoom_IdAndActiveTrue(user.getId(), roomId);
                    if (!enrolled) {
                        throw new IllegalArgumentException("Acesso negado: Aluno não matriculado nesta turma.");
                    }
                } else if (user.getRole() == UserRole.TEACHER) {
                    throw new IllegalArgumentException("Acesso negado: Professores não têm permissão para acessar o chat da turma.");
                } else {
                    throw new IllegalArgumentException("Acesso negado: Perfil não autorizado para o chat da turma.");
                }
            } else if (destination.equals("/topic/global/notifications") || destination.startsWith("/topic/global/")) {
                // Notificações globais da instituição: permitido a todos os usuários autenticados
                return message;
            } else if (destination.startsWith("/user/")) {
                // Filas privadas por sessão do próprio usuário (ex: /user/queue/errors, /user/queue/ai/reply)
                return message;
            } else {
                throw new AccessDeniedException("Acesso negado ao tópico solicitado.");
            }
        // 3. Envio de mensagens: restrito a destinos /app/ e requer autenticacao
        } else if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
            Principal principal = accessor.getUser();
            String destination = accessor.getDestination();
            if (principal == null || destination == null || !destination.startsWith("/app/")) {
                throw new AccessDeniedException("Destino de envio inválido ou usuário não autenticado.");
            }
        }
        return message;
    }
}
