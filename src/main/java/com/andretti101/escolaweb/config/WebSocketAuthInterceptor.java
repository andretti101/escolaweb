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

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtTokenProvider.extractUsername(token);
                if (username != null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtTokenProvider.validateToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                        accessor.setUser(authentication);
                    }
                }
            }
        } else if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Principal principal = accessor.getUser();
            if (principal == null) {
                throw new IllegalArgumentException("Usuário não autenticado no WebSocket.");
            }
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/classroom/")) {
                String roomIdStr = destination.substring("/topic/classroom/".length());
                Integer roomId = Integer.parseInt(roomIdStr);

                User user = userRepository.findByEmail(principal.getName())
                        .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

                // DIRETORIA ou SECRETARIA possuem acesso livre
                if (user.getRole() == UserRole.SECRETARY || user.getRole() == UserRole.PRINCIPAL) {
                    return message;
                }

                // Se for Aluno, valida a matrícula
                if (user.getRole() == UserRole.STUDENT) {
                    boolean enrolled = enrollmentRepository.existsByStudent_IdAndClassRoom_IdAndActiveTrue(user.getId(), roomId);
                    if (!enrolled) {
                        throw new IllegalArgumentException("Acesso negado: Aluno não matriculado nesta turma.");
                    }
                } else {
                    // Para outras roles não listadas (ex: TEACHER) que tentarem, lançar erro 
                    // (O prompt especificou apenas Aluno ou DIRETORIA/SECRETARIA)
                    throw new IllegalArgumentException("Acesso negado: Perfil não autorizado para o chat da turma.");
                }
            }
        }
        return message;
    }
}
