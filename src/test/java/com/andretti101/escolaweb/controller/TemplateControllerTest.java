package com.andretti101.escolaweb.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Público: /entrar e /login.html retornam 200 com a view login")
    void publicLoginRoutes() throws Exception {
        mockMvc.perform(get("/entrar"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));

        mockMvc.perform(get("/login.html"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("Público: /recuperar-senha e /redefinir-senha retornam 200 com as respectivas views")
    void publicPasswordRoutes() throws Exception {
        mockMvc.perform(get("/recuperar-senha"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot-password"));

        mockMvc.perform(get("/redefinir-senha"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"));
    }

    @Test
    @DisplayName("Não autenticado: acesso a rota em português redireciona para /entrar")
    void unauthenticatedPortugueseRouteRedirectsToEntrar() throws Exception {
        mockMvc.perform(get("/direcao/painel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/entrar"));
    }

    @Test
    @DisplayName("Não autenticado: acesso a rota legada .html redireciona para /login.html")
    void unauthenticatedLegacyHtmlRedirectsToLoginHtml() throws Exception {
        mockMvc.perform(get("/principal/dashboard.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/login.html"));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("Direção: rotas limpas em português e legadas renderizam templates da direção")
    void principalRoutes() throws Exception {
        mockMvc.perform(get("/direcao/painel"))
                .andExpect(status().isOk())
                .andExpect(view().name("principal/dashboard"));

        mockMvc.perform(get("/principal/dashboard.html"))
                .andExpect(status().isOk())
                .andExpect(view().name("principal/dashboard"));

        mockMvc.perform(get("/direcao/periodos-academicos"))
                .andExpect(status().isOk())
                .andExpect(view().name("principal/academic-periods"));

        mockMvc.perform(get("/direcao/cadastrar/periodo"))
                .andExpect(status().isOk())
                .andExpect(view().name("principal/create/academic-period"));

        mockMvc.perform(get("/direcao/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("principal/users"));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("Secretaria: rotas limpas em português e legadas renderizam templates da secretaria")
    void secretaryRoutes() throws Exception {
        mockMvc.perform(get("/secretaria/painel"))
                .andExpect(status().isOk())
                .andExpect(view().name("secretary/dashboard"));

        mockMvc.perform(get("/secretaria/usuarios"))
                .andExpect(status().isOk())
                .andExpect(view().name("secretary/users"));

        mockMvc.perform(get("/secretaria/alunos"))
                .andExpect(status().isOk())
                .andExpect(view().name("secretary/students"));

        mockMvc.perform(get("/secretaria/cadastrar/aluno"))
                .andExpect(status().isOk())
                .andExpect(view().name("secretary/create/student"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("Professor: rotas limpas em português e legadas renderizam templates do professor")
    void teacherRoutes() throws Exception {
        mockMvc.perform(get("/professor/painel"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/dashboard"));

        mockMvc.perform(get("/professor/aulas"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/lessons"));

        mockMvc.perform(get("/professor/cadastrar/aula"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/create/lesson"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Aluno: rotas limpas em português e legadas renderizam templates do aluno")
    void studentRoutes() throws Exception {
        mockMvc.perform(get("/aluno/painel"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/dashboard"));

        mockMvc.perform(get("/aluno/boletim"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/report-card"));

        mockMvc.perform(get("/aluno/notas"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/grades"));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("Cardápio: rotas de cardápio escolar renderizam os templates corretos")
    void mealMenuRoutes() throws Exception {
        mockMvc.perform(get("/direcao/cardapio"))
                .andExpect(status().isOk())
                .andExpect(view().name("principal/meal-menu"));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("Cardápio Secretaria: rota /secretaria/cardapio renderiza template da secretaria")
    void mealMenuSecretaryRoute() throws Exception {
        mockMvc.perform(get("/secretaria/cardapio"))
                .andExpect(status().isOk())
                .andExpect(view().name("secretary/meal-menu"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("Cardápio Professor: rota /professor/cardapio renderiza template do professor")
    void mealMenuTeacherRoute() throws Exception {
        mockMvc.perform(get("/professor/cardapio"))
                .andExpect(status().isOk())
                .andExpect(view().name("teacher/meal-menu"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Cardápio Aluno: rota /aluno/cardapio renderiza template do aluno")
    void mealMenuStudentRoute() throws Exception {
        mockMvc.perform(get("/aluno/cardapio"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/meal-menu"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Assistente IA: rota /assistente-ia autenticada renderiza ai-chat")
    void aiAssistantRoute() throws Exception {
        mockMvc.perform(get("/assistente-ia"))
                .andExpect(status().isOk())
                .andExpect(view().name("ai-chat"));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("Rota inexistente sob prefixo conhecido retorna 404 Not Found")
    void nonexistentRouteReturns404() throws Exception {
        mockMvc.perform(get("/direcao/rota-que-nao-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Raiz / sem autenticação redireciona para /entrar")
    void rootUnauthenticatedRedirectsToEntrar() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/entrar"));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("Raiz / autenticado como Direção redireciona para /direcao/painel")
    void rootPrincipalRedirectsToPainel() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/direcao/painel"));
    }

    @Test
    @WithMockUser(roles = "SECRETARY")
    @DisplayName("Raiz / autenticado como Secretaria redireciona para /secretaria/painel")
    void rootSecretaryRedirectsToPainel() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/secretaria/painel"));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("Raiz / autenticado como Professor redireciona para /professor/painel")
    void rootTeacherRedirectsToPainel() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/professor/painel"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Raiz / autenticado como Aluno redireciona para /aluno/painel")
    void rootStudentRedirectsToPainel() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/aluno/painel"));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("Atalhos de seção: /direcao e /direcao/ redirecionam para /direcao/painel")
    void sectionShortcutsRedirectToPainel() throws Exception {
        mockMvc.perform(get("/direcao"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/direcao/painel"));

        mockMvc.perform(get("/direcao/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/direcao/painel"));
    }

    @Test
    @WithMockUser(roles = "PRINCIPAL")
    @DisplayName("Rota com barra no final renderiza template normalmente")
    void routeWithTrailingSlash() throws Exception {
        mockMvc.perform(get("/direcao/painel/"))
                .andExpect(status().isOk())
                .andExpect(view().name("principal/dashboard"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    @DisplayName("Acesso negado: aluno tentando acessar rota da direção deve receber 403 Forbidden")
    void studentAccessingPrincipalRouteReturnsForbidden() throws Exception {
        mockMvc.perform(get("/direcao/painel"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("Acesso negado: professor tentando acessar rota da secretaria deve receber 403 Forbidden")
    void teacherAccessingSecretaryRouteReturnsForbidden() throws Exception {
        mockMvc.perform(get("/secretaria/painel"))
                .andExpect(status().isForbidden());
    }
}
