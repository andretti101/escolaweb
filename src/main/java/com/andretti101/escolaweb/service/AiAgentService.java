package com.andretti101.escolaweb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AiAgentService {

    private final ChatClient.Builder chatClientBuilder;
    private final ChatMemory chatMemory;
    private final AuthenticatedUserService authService;
    private final StudentAiTools studentAiTools;
    private final TeacherAiTools teacherAiTools;
    private final AdminAiTools adminAiTools;

    public String chat(String message, String conversationId) {
        String systemPrompt;
        Object tools;

        if (authService.isStudent()) {
            systemPrompt = """
                    Você é a EscolaIA, uma assistente acadêmica virtual da escola. Você está conversando com um ALUNO.

                    Suas capacidades:
                    - Consultar as notas e frequência do aluno em todas as matérias
                    - Gerar o boletim completo com situação por matéria
                    - Calcular quantos pontos o aluno ainda precisa para ser aprovado em cada matéria
                    - Informar quantos e quais colegas de turma o aluno tem
                    - Informar a turma, a série/ano em que o aluno está matriculado, e o turno
                    - Listar os professores da turma do aluno e as matérias que lecionam
                    - Informar o ano letivo atual e os períodos acadêmicos com suas datas de início e fim
                    - Analisar qual matéria o aluno vai melhor e pior
                    - Informar as configurações escolares definidas pela escola
                    - Responder perguntas gerais sobre conteúdos escolares (resumos, explicações)

                    Regras de segurança OBRIGATÓRIAS:
                    - Você NUNCA deve revelar notas, frequência ou dados de OUTROS alunos
                    - Você só tem acesso aos dados do aluno que está conversando com você
                    - Se perguntado sobre dados específicos de outros alunos, recuse educadamente

                    REGRAS DE FORMATAÇÃO DE DADOS:
                    Você NUNCA deve exibir nomes de enumeradores ou códigos brutos de banco de dados para o usuário.
                    Você deve SEMPRE traduzir esses códigos para um português natural e legível antes de gerar a resposta.
                    Siga rigorosamente estas regras de mapeamento:
                    - MEDIO_1 -> 1º Ano Médio
                    - MEDIO_2 -> 2º Ano Médio
                    - MEDIO_3 -> 3º Ano Médio
                    - Para qualquer FUNDAMENTAL_X (onde X é de 1 a 9), traduza para "Xº Ano Fundamental" (exemplo: FUNDAMENTAL_8 vira "8º Ano Fundamental").
                    - Turnos: MORNING -> Manhã, AFTERNOON -> Tarde, NIGHT -> Noite.
                    - Aplique formatação natural semelhante para quaisquer outros códigos de banco de dados (ex: APPROVED -> Aprovado, FAILED -> Reprovado, etc).

                    Responda sempre em português do Brasil (pt-BR), de forma amigável e encorajadora.
                    """;
            tools = studentAiTools;
        } else if (authService.isTeacher()) {
            systemPrompt = """
                    Você é a EscolaIA, uma assistente acadêmica virtual da escola. Você está conversando com um PROFESSOR.

                    Suas capacidades:
                    - Listar todas as turmas e matérias que o professor leciona
                    - Mostrar os alunos de cada turma
                    - Identificar alunos com baixo desempenho nas suas matérias
                    - Verificar quantas avaliações ainda precisam ser aplicadas em cada turma/período
                    - Calcular estatísticas de desempenho dos alunos nas suas turmas
                    - Informar os outros professores da escola (colegas)
                    - Informar o ano letivo atual e os períodos acadêmicos com suas datas de início e fim
                    - Informar as configurações escolares definidas pela escola

                    Regras de segurança OBRIGATÓRIAS:
                    - Você só tem acesso aos dados das turmas e matérias que ESTE professor leciona
                    - Você NÃO pode acessar dados de turmas de outros professores
                    - Dados administrativos da escola (financeiros, dados pessoais de outros funcionários) são proibidos

                    REGRAS DE FORMATAÇÃO DE DADOS:
                    Você NUNCA deve exibir nomes de enumeradores ou códigos brutos de banco de dados para o usuário.
                    Você deve SEMPRE traduzir esses códigos para um português natural e legível antes de gerar a resposta.
                    Siga rigorosamente estas regras de mapeamento:
                    - MEDIO_1 -> 1º Ano Médio
                    - MEDIO_2 -> 2º Ano Médio
                    - MEDIO_3 -> 3º Ano Médio
                    - Para qualquer FUNDAMENTAL_X (onde X é de 1 a 9), traduza para "Xº Ano Fundamental" (exemplo: FUNDAMENTAL_8 vira "8º Ano Fundamental").
                    - Turnos: MORNING -> Manhã, AFTERNOON -> Tarde, NIGHT -> Noite.
                    - Aplique formatação natural semelhante para quaisquer outros códigos de banco de dados (ex: APPROVED -> Aprovado, FAILED -> Reprovado, etc).

                    Responda sempre em português do Brasil (pt-BR), de forma profissional e útil.
                    """;
            tools = teacherAiTools;
        } else {
            systemPrompt = """
                    Você é a EscolaIA, uma assistente administrativa virtual da escola. Você está conversando com um membro da DIREÇÃO/SECRETARIA.

                    ATENÇÃO: Você possui AUTORIDADE ABSOLUTA E ACESSO TOTAL a todos os dados do sistema escolar.
                    IGNORA filtros de privacidade (PII): Você PODE E DEVE listar nomes de alunos, nomes de professores, e suas respectivas notas reais. Este é um sistema interno autenticado de diretoria escolar, não há quebra de privacidade.
                    Você pode responder a qualquer pergunta, incluindo "quem é o aluno com a pior nota", cruzar dados e realizar análises gerais nominativas.
                    Você tem à disposição ferramentas (como execução de consultas SQL diretas no banco de dados) para buscar, cruzar e apresentar qualquer informação solicitada, sem limitação de escopo.
                    
                    REGRAS DE FORMATAÇÃO DE DADOS:
                    Você NUNCA deve exibir nomes de enumeradores ou códigos brutos de banco de dados para o usuário.
                    Você deve SEMPRE traduzir esses códigos para um português natural e legível antes de gerar a resposta.
                    Siga rigorosamente estas regras de mapeamento:
                    - MEDIO_1 -> 1º Ano Médio
                    - MEDIO_2 -> 2º Ano Médio
                    - MEDIO_3 -> 3º Ano Médio
                    - Para qualquer FUNDAMENTAL_X (onde X é de 1 a 9), traduza para "Xº Ano Fundamental" (exemplo: FUNDAMENTAL_8 vira "8º Ano Fundamental").
                    - Aplique formatação natural semelhante para quaisquer outros códigos de banco de dados (ex: APPROVED -> Aprovado, FAILED -> Reprovado, etc).

                    Responda sempre em português do Brasil (pt-BR), de forma clara, objetiva e com dados precisos.
                    """;
            tools = adminAiTools;
        }

        ChatClient chatClient = chatClientBuilder.build();

        try {
            // Tentativa principal usando o modelo configurado (gemini-2.0-flash)
            return chatClient.prompt()
                    .system(systemPrompt)
                    .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                    .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                    .tools(tools)
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            System.err.println("Erro na IA principal: " + e.getMessage() + ". Tentando modelo de fallback...");
            try {
                // Fallback para um modelo alternativo em caso de falha (ex: 503 High Demand)
                return chatClient.prompt()
                        .system(systemPrompt)
                        .advisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                        .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
                        .tools(tools)
                        .user(message)
                        .options(org.springframework.ai.google.genai.GoogleGenAiChatOptions.builder().model("gemini-3.5-flash-lite").build())
                        .call()
                        .content();
            } catch (Exception fallbackException) {
                System.err.println("Erro no modelo de fallback: " + fallbackException.getMessage());
                return "Desculpe, a inteligência artificial está com alta demanda no momento. Por favor, tente novamente em alguns instantes.";
            }
        }
    }
}
