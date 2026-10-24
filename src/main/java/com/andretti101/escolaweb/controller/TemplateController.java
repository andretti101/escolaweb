package com.andretti101.escolaweb.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.ResourceLoader;
import lombok.RequiredArgsConstructor;

// Controlador MVC que resolve URLs amigáveis em português para as respectivas visualizações Thymeleaf
@Hidden
@Controller
@RequiredArgsConstructor
public class TemplateController {

    private final ResourceLoader resourceLoader;

    // Mapeamento bidirecional entre rotas amigáveis (em português e inglês) e os caminhos de templates
    private static final Map<String, String> PORTUGUESE_ROUTES;

    static {
        Map<String, String> routes = new LinkedHashMap<>();

        // Públicos
        routes.put("/entrar", "login");
        routes.put("/login", "login");
        routes.put("/recuperar-senha", "forgot-password");
        routes.put("/redefinir-senha", "reset-password");
        routes.put("/assistente-ia", "ai-chat");

        // Direção
        routes.put("/direcao/painel", "principal/dashboard");
        routes.put("/direcao/periodos-academicos", "principal/academic-periods");
        routes.put("/direcao/anos-letivos", "principal/academic-years");
        routes.put("/direcao/conclusao-ano", "principal/year-conclusion");
        routes.put("/direcao/resultado-conclusao-ano", "principal/year-conclusion-results");
        routes.put("/direcao/turmas", "principal/classrooms");
        routes.put("/direcao/usuarios", "principal/users");
        routes.put("/direcao/professores", "principal/teachers");
        routes.put("/direcao/alunos", "principal/students");
        routes.put("/direcao/desempenho-aluno", "principal/student-performance");
        routes.put("/direcao/disciplinas", "principal/subjects");
        routes.put("/direcao/diretores", "principal/principals");
        routes.put("/direcao/secretarios", "principal/secretaries");
        routes.put("/direcao/avisos", "principal/announcements");
        routes.put("/direcao/meus-avisos", "principal/my-announcements");
        routes.put("/direcao/avisos-arquivados", "principal/archived-announcements");
        routes.put("/direcao/chat", "principal/chat");
        routes.put("/direcao/chat-arquivado", "principal/archived-chat");
        routes.put("/direcao/historico-alteracoes", "principal/change-history");
        routes.put("/direcao/configuracoes-escola", "principal/school-settings");
        routes.put("/direcao/cadastrar/periodo", "principal/create/academic-period");
        routes.put("/direcao/cadastrar/ano-letivo", "principal/create/academic-year");
        routes.put("/direcao/cadastrar/aviso", "principal/create/announcement");
        routes.put("/direcao/editar/periodo", "principal/edit/academic-period");
        routes.put("/direcao/editar/ano-letivo", "principal/edit/academic-year");
        routes.put("/direcao/editar/aviso", "principal/edit/announcement");
        routes.put("/direcao/grade-horaria", "principal/timetable");
        routes.put("/direcao/cardapio", "principal/meal-menu");

        // Secretaria
        routes.put("/secretaria/painel", "secretary/dashboard");
        routes.put("/secretaria/usuarios", "secretary/users");
        routes.put("/secretaria/alunos", "secretary/students");
        routes.put("/secretaria/desempenho-aluno", "secretary/student-performance");
        routes.put("/secretaria/turmas", "secretary/classrooms");
        routes.put("/secretaria/gerenciar-turma", "secretary/manage-classroom");
        routes.put("/secretaria/professores", "secretary/teachers");
        routes.put("/secretaria/disciplinas", "secretary/subjects");
        routes.put("/secretaria/periodos-academicos", "secretary/academic-periods");
        routes.put("/secretaria/anos-letivos", "secretary/academic-years");
        routes.put("/secretaria/resultado-conclusao-ano", "secretary/year-conclusion-results");
        routes.put("/secretaria/diretores", "secretary/principals");
        routes.put("/secretaria/secretarios", "secretary/secretaries");
        routes.put("/secretaria/avisos", "secretary/announcements");
        routes.put("/secretaria/meus-avisos", "secretary/my-announcements");
        routes.put("/secretaria/avisos-arquivados", "secretary/archived-announcements");
        routes.put("/secretaria/chat", "secretary/chat");
        routes.put("/secretaria/chat-arquivado", "secretary/archived-chat");
        routes.put("/secretaria/historico-alteracoes", "secretary/change-history");
        routes.put("/secretaria/configuracoes-escola", "secretary/school-settings");
        routes.put("/secretaria/cadastrar/aluno", "secretary/create/student");
        routes.put("/secretaria/cadastrar/turma", "secretary/create/classroom");
        routes.put("/secretaria/cadastrar/professor", "secretary/create/teacher");
        routes.put("/secretaria/cadastrar/disciplina", "secretary/create/subject");
        routes.put("/secretaria/cadastrar/diretor", "secretary/create/principal");
        routes.put("/secretaria/cadastrar/secretario", "secretary/create/secretary");
        routes.put("/secretaria/cadastrar/aviso", "secretary/create/announcement");
        routes.put("/secretaria/editar/aluno", "secretary/edit/student");
        routes.put("/secretaria/editar/turma", "secretary/edit/classroom");
        routes.put("/secretaria/editar/professor", "secretary/edit/teacher");
        routes.put("/secretaria/editar/disciplina", "secretary/edit/subject");
        routes.put("/secretaria/editar/diretor", "secretary/edit/principal");
        routes.put("/secretaria/editar/secretario", "secretary/edit/secretary");
        routes.put("/secretaria/editar/aviso", "secretary/edit/announcement");
        routes.put("/secretaria/grade-horaria", "secretary/timetable");
        routes.put("/secretaria/cardapio", "secretary/meal-menu");

        // Professor
        routes.put("/professor/painel", "teacher/dashboard");
        routes.put("/professor/aulas", "teacher/lessons");
        routes.put("/professor/avaliacoes", "teacher/assessments");
        routes.put("/professor/meus-alunos", "teacher/my-students");
        routes.put("/professor/colegas", "teacher/colleagues");
        routes.put("/professor/avisos", "teacher/announcements");
        routes.put("/professor/meus-avisos", "teacher/my-announcements");
        routes.put("/professor/avisos-direcionados", "teacher/targeted-announcements");
        routes.put("/professor/chat", "teacher/chat");
        routes.put("/professor/periodos-academicos", "teacher/academic-periods");
        routes.put("/professor/anos-letivos", "teacher/academic-years");
        routes.put("/professor/historico-alteracoes", "teacher/change-history");
        routes.put("/professor/configuracoes-escola", "teacher/school-settings");
        routes.put("/professor/cadastrar/aula", "teacher/create/lesson");
        routes.put("/professor/cadastrar/avaliacao", "teacher/create/assessment");
        routes.put("/professor/cadastrar/aviso", "teacher/create/announcement");
        routes.put("/professor/editar/aula", "teacher/edit/lesson");
        routes.put("/professor/editar/avaliacao", "teacher/edit/assessment");
        routes.put("/professor/editar/aviso", "teacher/edit/announcement");
        routes.put("/professor/grade-horaria", "teacher/timetable");
        routes.put("/professor/cardapio", "teacher/meal-menu");

        // Aluno
        routes.put("/aluno/painel", "student/dashboard");
        routes.put("/aluno/boletim", "student/report-card");
        routes.put("/aluno/notas", "student/grades");
        routes.put("/aluno/frequencias", "student/attendances");
        routes.put("/aluno/colegas", "student/classmates");
        routes.put("/aluno/disciplinas", "student/subjects");
        routes.put("/aluno/avisos", "student/announcements");
        routes.put("/aluno/chat", "student/chat");
        routes.put("/aluno/periodos-academicos", "student/academic-periods");
        routes.put("/aluno/anos-letivos", "student/academic-years");
        routes.put("/aluno/configuracoes-escola", "student/school-settings");
        routes.put("/aluno/grade-horaria", "student/timetable");
        routes.put("/aluno/cardapio", "student/meal-menu");

        PORTUGUESE_ROUTES = Collections.unmodifiableMap(routes);
    }

    @GetMapping("/")
    public String handleRoot(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_PRINCIPAL".equals(role)) {
                    return "redirect:/direcao/painel";
                } else if ("ROLE_SECRETARY".equals(role)) {
                    return "redirect:/secretaria/painel";
                } else if ("ROLE_TEACHER".equals(role)) {
                    return "redirect:/professor/painel";
                } else if ("ROLE_STUDENT".equals(role)) {
                    return "redirect:/aluno/painel";
                }
            }
        }
        return "redirect:/entrar";
    }

    @GetMapping({
            "/entrar",
            "/login",
            "/recuperar-senha",
            "/redefinir-senha",
            "/assistente-ia",
            "/{role:direcao|secretaria|professor|aluno}",
            "/{role:direcao|secretaria|professor|aluno}/**"
    })
    public String handlePortugueseRoute(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (path != null && path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if ("/direcao".equals(path)) {
            return "redirect:/direcao/painel";
        }
        if ("/secretaria".equals(path)) {
            return "redirect:/secretaria/painel";
        }
        if ("/professor".equals(path)) {
            return "redirect:/professor/painel";
        }
        if ("/aluno".equals(path)) {
            return "redirect:/aluno/painel";
        }

        String template = PORTUGUESE_ROUTES.get(path);
        if (template != null) {
            return template;
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @GetMapping({
            "/login.html",
            "/forgot-password.html",
            "/reset-password.html",
            "/ai-chat.html",
            "/{dir:principal|secretary|teacher|student|components}/{page:[a-zA-Z0-9_-]+}.html",
            "/{dir:principal|secretary|teacher|student|components}/{sub:[a-zA-Z0-9_-]+}/{page:[a-zA-Z0-9_-]+}.html"
    })
    public String handleHtml(HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String template = path.substring(1, path.length() - 5);
        var resource = resourceLoader.getResource("classpath:/templates/" + template + ".html");
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return template;
    }
}
