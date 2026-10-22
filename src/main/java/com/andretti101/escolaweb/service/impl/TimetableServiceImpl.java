package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.dto.request.TimetableGenerationRequestDTO;
import com.andretti101.escolaweb.dto.request.TimetableSlotRequestDTO;
import com.andretti101.escolaweb.dto.response.TimetableGenerationResponseDTO;
import com.andretti101.escolaweb.dto.response.TimetableSlotResponseDTO;
import com.andretti101.escolaweb.dto.response.WeeklyScheduleDTO;
import com.andretti101.escolaweb.model.entity.*;
import com.andretti101.escolaweb.model.enums.DayOfWeekEnum;
import com.andretti101.escolaweb.model.enums.Shift;
import com.andretti101.escolaweb.repository.*;
import com.andretti101.escolaweb.service.AuthenticatedUserService;
import com.andretti101.escolaweb.service.TimetableService;
import com.andretti101.escolaweb.service.exception.ConflictException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

// Serviço de gestão da grade horária escolar e geração automatizada de horários sem conflitos
@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableServiceImpl implements TimetableService {

    private final TimetableSlotRepository timetableSlotRepository;
    private final ClassRoomRepository classRoomRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final TeacherRepository teacherRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final AuthenticatedUserService authenticatedUserService;

    // Adiciona um horário na grade checando conflito de professor ocupado e sala ocupada
    @Override
    @Transactional
    public TimetableSlotResponseDTO addSlot(TimetableSlotRequestDTO dto) {
        validateTimeSlot(dto);

        ClassRoom classRoom = findClassRoom(dto.classRoomId());
        TeacherClassSubject tcs = findTeacherClassSubject(dto.teacherClassSubjectId());

        validateClassRoomAndTeacherClassSubject(classRoom, tcs);
        // Previne choque de horário: nem a turma nem o docente podem ter outro compromisso no mesmo intervalo
        checkConflicts(classRoom, tcs, dto.dayOfWeek(), dto.startTime(), dto.endTime(), null);

        TimetableSlot slot = TimetableSlot.builder()
                .classRoom(classRoom)
                .teacherClassSubject(tcs)
                .dayOfWeek(dto.dayOfWeek())
                .startTime(dto.startTime())
                .endTime(dto.endTime())
                .slotOrder(dto.slotOrder())
                .build();

        slot = timetableSlotRepository.save(slot);
        log.info("Timetable slot created: id={}, classroom={}, teacher={}, day={}, start={}, end={}",
                slot.getId(), classRoom.getName(), tcs.getTeacher().getName(), dto.dayOfWeek(), dto.startTime(), dto.endTime());
        return TimetableSlotResponseDTO.fromEntity(slot);
    }

    @Override
    @Transactional
    public TimetableSlotResponseDTO updateSlot(Integer id, TimetableSlotRequestDTO dto) {
        validateTimeSlot(dto);

        TimetableSlot slot = timetableSlotRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Horário de aula não encontrado com o ID: " + id));

        ClassRoom classRoom = findClassRoom(dto.classRoomId());
        TeacherClassSubject tcs = findTeacherClassSubject(dto.teacherClassSubjectId());

        validateClassRoomAndTeacherClassSubject(classRoom, tcs);
        checkConflicts(classRoom, tcs, dto.dayOfWeek(), dto.startTime(), dto.endTime(), id);

        slot.setClassRoom(classRoom);
        slot.setTeacherClassSubject(tcs);
        slot.setDayOfWeek(dto.dayOfWeek());
        slot.setStartTime(dto.startTime());
        slot.setEndTime(dto.endTime());
        slot.setSlotOrder(dto.slotOrder());

        slot = timetableSlotRepository.save(slot);
        log.info("Timetable slot updated: id={}, classroom={}, teacher={}, day={}, start={}, end={}",
                slot.getId(), classRoom.getName(), tcs.getTeacher().getName(), dto.dayOfWeek(), dto.startTime(), dto.endTime());
        return TimetableSlotResponseDTO.fromEntity(slot);
    }

    @Override
    @Transactional
    public void removeSlot(Integer slotId) {
        TimetableSlot slot = timetableSlotRepository.findById(slotId)
                .orElseThrow(() -> new EntityNotFoundException("Horário de aula não encontrado com o ID: " + slotId));
        timetableSlotRepository.delete(slot);
        log.info("Timetable slot removed: id={}", slotId);
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyScheduleDTO getScheduleByClassroom(Integer classroomId) {
        ClassRoom classRoom = findClassRoom(classroomId);
        List<TimetableSlot> slots = timetableSlotRepository.findByClassRoom_IdOrderByDayOfWeekAscSlotOrderAscStartTimeAsc(classroomId);
        return buildWeeklySchedule(classRoom.getId(), classRoom.getName(), null, null, slots);
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyScheduleDTO getScheduleForAuthenticatedStudent() {
        Student student = authenticatedUserService.getAuthenticatedStudent();
        List<Enrollment> enrollments = enrollmentRepository.findByStudentAndActiveTrue(student);
        if (enrollments.isEmpty()) {
            return new WeeklyScheduleDTO(null, "Sem turma ativa", null, null, List.of(), createEmptyDayMap());
        }

        Enrollment activeEnrollment = enrollments.stream()
                .filter(e -> e.getClassRoom() != null && e.getClassRoom().getAcademicYear() != null && e.getClassRoom().getAcademicYear().isActive())
                .findFirst()
                .orElse(enrollments.get(0));

        if (activeEnrollment.getClassRoom() == null) {
            return new WeeklyScheduleDTO(null, "Sem turma ativa", null, null, List.of(), createEmptyDayMap());
        }

        return getScheduleByClassroom(activeEnrollment.getClassRoom().getId());
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyScheduleDTO getScheduleForAuthenticatedTeacher() {
        return getScheduleForAuthenticatedTeacher(null);
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyScheduleDTO getScheduleForAuthenticatedTeacher(Integer academicYearId) {
        Teacher teacher = authenticatedUserService.getAuthenticatedTeacher();
        return getScheduleByTeacher(teacher.getId(), academicYearId);
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyScheduleDTO getScheduleByTeacher(Integer teacherId) {
        return getScheduleByTeacher(teacherId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public WeeklyScheduleDTO getScheduleByTeacher(Integer teacherId, Integer academicYearId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new EntityNotFoundException("Professor não encontrado com o ID: " + teacherId));

        Integer targetYearId = academicYearId;
        if (targetYearId == null) {
            targetYearId = academicYearRepository.findByActiveTrue()
                    .map(AcademicYear::getId)
                    .orElse(null);
        }

        List<TimetableSlot> slots = timetableSlotRepository.findByTeacherAndAcademicYear(teacherId, targetYearId);
        return buildWeeklySchedule(null, null, teacher.getId(), teacher.getName(), slots);
    }

    // ── Helper methods

    private void validateTimeSlot(TimetableSlotRequestDTO dto) {
        if (dto.startTime() == null || dto.endTime() == null) {
            throw new IllegalArgumentException("Os horários de início e término são obrigatórios.");
        }
        if (!dto.startTime().isBefore(dto.endTime())) {
            throw new IllegalArgumentException("O horário de início deve ser estritamente anterior ao horário de término.");
        }
    }

    private ClassRoom findClassRoom(Integer classRoomId) {
        return classRoomRepository.findById(classRoomId)
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada com o ID: " + classRoomId));
    }

    private TeacherClassSubject findTeacherClassSubject(Integer tcsId) {
        return teacherClassSubjectRepository.findById(tcsId)
                .orElseThrow(() -> new EntityNotFoundException("Vinculação de professor e matéria não encontrada com o ID: " + tcsId));
    }

    private void validateClassRoomAndTeacherClassSubject(ClassRoom classRoom, TeacherClassSubject tcs) {
        if (!classRoom.isActive()) {
            throw new IllegalArgumentException("Não é possível gerenciar horários de uma turma inativa.");
        }
        if (!tcs.isActive()) {
            throw new IllegalArgumentException("A vinculação de professor e matéria informada está inativa.");
        }
        if (!tcs.getClassRoom().getId().equals(classRoom.getId())) {
            throw new IllegalArgumentException("A vinculação de professor e matéria informada não pertence à turma " + classRoom.getName() + ".");
        }
    }

    private void checkConflicts(ClassRoom classRoom, TeacherClassSubject tcs,
                                DayOfWeekEnum dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime,
                                Integer excludeSlotId) {
        // 1. Conflito da turma (mesma turma com matérias sobrepostas)
        List<TimetableSlot> classCollisions = timetableSlotRepository.findClassRoomCollisions(
                classRoom.getId(), dayOfWeek, startTime, endTime, excludeSlotId);
        if (!classCollisions.isEmpty()) {
            TimetableSlot coll = classCollisions.get(0);
            String existingSubject = coll.getTeacherClassSubject() != null && coll.getTeacherClassSubject().getSubject() != null
                    ? coll.getTeacherClassSubject().getSubject().getName()
                    : "outra disciplina";
            throw new ConflictException(String.format(
                    "Choque de horário na turma: A turma '%s' já possui a aula de '%s' agendada na %s no horário de %s às %s.",
                    classRoom.getName(), existingSubject, dayOfWeek.getLabel(), coll.getStartTime(), coll.getEndTime()));
        }

        // 2. Conflito do professor (mesmo professor em turmas sobrepostas no mesmo ano letivo)
        Integer teacherId = tcs.getTeacher().getId();
        Integer academicYearId = classRoom.getAcademicYear() != null ? classRoom.getAcademicYear().getId() : null;
        List<TimetableSlot> teacherCollisions = timetableSlotRepository.findTeacherCollisions(
                teacherId, academicYearId, dayOfWeek, startTime, endTime, excludeSlotId);
        if (!teacherCollisions.isEmpty()) {
            TimetableSlot coll = teacherCollisions.get(0);
            String teacherName = tcs.getTeacher().getName();
            String otherClass = coll.getClassRoom() != null ? coll.getClassRoom().getName() : "outra turma";
            String otherSubject = coll.getTeacherClassSubject() != null && coll.getTeacherClassSubject().getSubject() != null
                    ? coll.getTeacherClassSubject().getSubject().getName()
                    : "disciplina";
            throw new ConflictException(String.format(
                    "Choque de horário do professor: O(A) professor(a) '%s' já possui a aula de '%s' agendada na turma '%s' na %s no horário de %s às %s.",
                    teacherName, otherSubject, otherClass, dayOfWeek.getLabel(), coll.getStartTime(), coll.getEndTime()));
        }
    }

    private WeeklyScheduleDTO buildWeeklySchedule(Integer classRoomId, String classRoomName,
                                                  Integer teacherId, String teacherName,
                                                  List<TimetableSlot> slots) {
        List<TimetableSlot> sortedSlots = slots.stream()
                .sorted(Comparator.comparing(TimetableSlot::getDayOfWeek, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TimetableSlot::getSlotOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TimetableSlot::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<TimetableSlotResponseDTO> dtoList = sortedSlots.stream()
                .map(TimetableSlotResponseDTO::fromEntity)
                .toList();

        Map<DayOfWeekEnum, List<TimetableSlotResponseDTO>> slotsByDay = createEmptyDayMap();
        for (TimetableSlotResponseDTO slotDTO : dtoList) {
            if (slotDTO.dayOfWeek() != null) {
                slotsByDay.computeIfAbsent(slotDTO.dayOfWeek(), k -> new ArrayList<>()).add(slotDTO);
            }
        }

        return new WeeklyScheduleDTO(classRoomId, classRoomName, teacherId, teacherName, dtoList, slotsByDay);
    }

    private Map<DayOfWeekEnum, List<TimetableSlotResponseDTO>> createEmptyDayMap() {
        Map<DayOfWeekEnum, List<TimetableSlotResponseDTO>> map = new LinkedHashMap<>();
        for (DayOfWeekEnum day : DayOfWeekEnum.values()) {
            map.put(day, new ArrayList<>());
        }
        return map;
    }

    // ── Geração Automática da Grade Horária

    private record SlotDefinition(DayOfWeekEnum dayOfWeek, int slotOrder, java.time.LocalTime startTime, java.time.LocalTime endTime) {}
    private record SlotTime(java.time.LocalTime startTime, java.time.LocalTime endTime) {}
    private record TcsGroup(int groupIndex, TeacherClassSubject tcs, ClassRoom classRoom, Teacher teacher, Subject subject, int totalUnits) {}
    private record ClassRoomSlotKey(Integer classRoomId, DayOfWeekEnum dayOfWeek, int slotOrder) {}
    private record TimeInterval(DayOfWeekEnum dayOfWeek, java.time.LocalTime startTime, java.time.LocalTime endTime, Integer classRoomId) {}
    private record ClassRoomDaySubjectKey(Integer classRoomId, DayOfWeekEnum dayOfWeek, Integer subjectId) {}
    private record SlotCandidate(SlotDefinition slot, int score) {}

    private static final int SHIFT_BREAK_MINUTES = 10;
    private static final int LESSONS_BEFORE_BREAK = 3;

    @Override
    @Transactional
    public TimetableGenerationResponseDTO generateTimetable(TimetableGenerationRequestDTO dto) {
        validateGenerationRequest(dto);

        AcademicYear academicYear = resolveAcademicYear(dto.academicYearId());

        List<ClassRoom> targetClassrooms = resolveTargetClassrooms(academicYear, dto.classRoomIds());
        if (targetClassrooms.isEmpty()) {
            throw new IllegalStateException("Nenhuma turma ativa encontrada para a geração da grade horária.");
        }

        Set<Integer> sixthLessonClassroomIds = new HashSet<>(
                dto.classroomsWithSixthLessonIds() != null ? dto.classroomsWithSixthLessonIds() : List.of()
        );

        // Validação da regra de negócio: apenas turmas do matutino e vespertino podem ter 6ª aula
        for (ClassRoom cr : targetClassrooms) {
            if (sixthLessonClassroomIds.contains(cr.getId())) {
                if (cr.getShift() != Shift.MORNING && cr.getShift() != Shift.AFTERNOON) {
                    throw new IllegalArgumentException(String.format(
                            "Apenas turmas dos turnos Matutino e Vespertino podem possuir a 6ª aula. A turma '%s' pertence ao turno %s.",
                            cr.getName(), cr.getShift().getLabel()));
                }
            }
        }

        List<DayOfWeekEnum> weekdays = List.of(
                DayOfWeekEnum.SEGUNDA, DayOfWeekEnum.TERCA, DayOfWeekEnum.QUARTA, DayOfWeekEnum.QUINTA, DayOfWeekEnum.SEXTA
        );

        int breakMinutes = dto.breakDurationMinutes() != null ? dto.breakDurationMinutes() : SHIFT_BREAK_MINUTES;

        java.time.LocalTime morningStart = dto.morningStartTime() != null
                ? dto.morningStartTime()
                : (dto.startTime() != null && dto.startTime().getHour() < 12 ? dto.startTime() : java.time.LocalTime.of(7, 50));

        java.time.LocalTime afternoonStart = dto.afternoonStartTime() != null
                ? dto.afternoonStartTime()
                : (dto.startTime() != null && dto.startTime().getHour() >= 12 && dto.startTime().getHour() < 18
                        ? dto.startTime()
                        : java.time.LocalTime.of(13, 15));

        java.time.LocalTime nightStart = dto.nightStartTime() != null
                ? dto.nightStartTime()
                : (dto.startTime() != null && dto.startTime().getHour() >= 18
                        ? dto.startTime()
                        : java.time.LocalTime.of(19, 0));

        java.time.LocalTime morningEnd = dto.morningEndTime();
        if (morningEnd == null && dto.lessonDurationMinutes() == null) {
            morningEnd = java.time.LocalTime.of(11, 45);
        }

        java.time.LocalTime afternoonEnd = dto.afternoonEndTime();
        if (afternoonEnd == null && dto.lessonDurationMinutes() == null) {
            afternoonEnd = java.time.LocalTime.of(17, 10);
        }

        java.time.LocalTime nightEnd = dto.nightEndTime();
        if (nightEnd == null && dto.lessonDurationMinutes() == null) {
            nightEnd = java.time.LocalTime.of(22, 30);
        }

        Map<ClassRoom, List<SlotDefinition>> classroomSlots = new LinkedHashMap<>();

        for (ClassRoom cr : targetClassrooms) {
            List<SlotDefinition> slots = new ArrayList<>();

            if (cr.getShift() == Shift.FULL_TIME) {
                List<SlotTime> mTimes = computeShiftSlotTimes(morningStart, morningEnd, 5, breakMinutes, dto.lessonDurationMinutes(), false);
                java.time.LocalTime effectiveAfternoonStart = afternoonStart;
                java.time.LocalTime morningFinish = mTimes.get(mTimes.size() - 1).endTime();
                if (!effectiveAfternoonStart.isAfter(morningFinish)) {
                    effectiveAfternoonStart = morningFinish.plusMinutes(30);
                }
                java.time.LocalTime effectiveAfternoonEnd = afternoonEnd;
                if (effectiveAfternoonEnd != null && !effectiveAfternoonEnd.isAfter(effectiveAfternoonStart)) {
                    effectiveAfternoonEnd = effectiveAfternoonStart.plusMinutes(
                            morningEnd != null
                                    ? java.time.Duration.between(morningStart, morningEnd).toMinutes()
                                    : 5L * (dto.lessonDurationMinutes() != null ? dto.lessonDurationMinutes() : 45) + breakMinutes
                    );
                }
                List<SlotTime> aTimes = computeShiftSlotTimes(effectiveAfternoonStart, effectiveAfternoonEnd, 5, breakMinutes, dto.lessonDurationMinutes(), false);

                for (DayOfWeekEnum day : weekdays) {
                    for (int order = 1; order <= 5; order++) {
                        SlotTime st = mTimes.get(order - 1);
                        slots.add(new SlotDefinition(day, order, st.startTime(), st.endTime()));
                    }
                    for (int order = 6; order <= 10; order++) {
                        SlotTime st = aTimes.get(order - 6);
                        slots.add(new SlotDefinition(day, order, st.startTime(), st.endTime()));
                    }
                }
            } else if (cr.getShift() == Shift.EVENING) {
                // Turmas noturnas não possuem 6ª aula (sempre 5 aulas por dia)
                List<SlotTime> nTimes = computeShiftSlotTimes(nightStart, nightEnd, 5, breakMinutes, dto.lessonDurationMinutes(), true);
                for (DayOfWeekEnum day : weekdays) {
                    for (int order = 1; order <= 5; order++) {
                        SlotTime st = nTimes.get(order - 1);
                        slots.add(new SlotDefinition(day, order, st.startTime(), st.endTime()));
                    }
                }
            } else if (cr.getShift() == Shift.AFTERNOON) {
                int slotsPerDay = sixthLessonClassroomIds.contains(cr.getId()) ? 6 : 5;
                List<SlotTime> aTimes = computeShiftSlotTimes(afternoonStart, afternoonEnd, slotsPerDay, breakMinutes, dto.lessonDurationMinutes(), false);
                for (DayOfWeekEnum day : weekdays) {
                    for (int order = 1; order <= slotsPerDay; order++) {
                        SlotTime st = aTimes.get(order - 1);
                        slots.add(new SlotDefinition(day, order, st.startTime(), st.endTime()));
                    }
                }
            } else { // MORNING
                int slotsPerDay = sixthLessonClassroomIds.contains(cr.getId()) ? 6 : 5;
                List<SlotTime> mTimes = computeShiftSlotTimes(morningStart, morningEnd, slotsPerDay, breakMinutes, dto.lessonDurationMinutes(), false);
                for (DayOfWeekEnum day : weekdays) {
                    for (int order = 1; order <= slotsPerDay; order++) {
                        SlotTime st = mTimes.get(order - 1);
                        slots.add(new SlotDefinition(day, order, st.startTime(), st.endTime()));
                    }
                }
            }
            classroomSlots.put(cr, slots);
        }

        List<TcsGroup> allGroups = new ArrayList<>();
        int groupSeq = 0;
        Map<Integer, Integer> teacherLessonCount = new HashMap<>();

        for (ClassRoom cr : targetClassrooms) {
            List<TeacherClassSubject> tcsList = teacherClassSubjectRepository.findByClassRoomAndActiveTrue(cr);
            if (tcsList.isEmpty()) {
                continue;
            }

            int slotsPerDay = cr.getShift() == Shift.FULL_TIME
                    ? 10
                    : (sixthLessonClassroomIds.contains(cr.getId()) ? 6 : 5);
            int maxAvailable = slotsPerDay * weekdays.size();

            Map<Integer, Integer> effectiveWeeklyByTcs = new LinkedHashMap<>();
            int classroomNeeded = 0;
            for (TeacherClassSubject tcs : tcsList) {
                int weekly = tcs.getWeeklyLessons() != null ? tcs.getWeeklyLessons() : 2;
                effectiveWeeklyByTcs.put(tcs.getId(), weekly);
                classroomNeeded += weekly;
            }

            // Se a turma possui grade curricular completa (>= 6 disciplinas) e precisa preencher todos os períodos
            // (ex.: turma marcada com 6ª aula para 30 aulas/semana, turma regular 25 aulas/semana, ou turma integral 50 aulas/semana)
            if (tcsList.size() >= 6 && classroomNeeded < maxAvailable) {
                Map<Integer, Integer> addedByTcs = new HashMap<>();
                while (classroomNeeded < maxAvailable) {
                    TeacherClassSubject bestTcs = null;
                    int minAdded = Integer.MAX_VALUE;
                    int bestCurWeekly = -1;
                    int minTeacherLoad = Integer.MAX_VALUE;

                    // Priorizar disciplinas com < 4 aulas/semana, distribuindo +1 aula por disciplina antes de repetir
                    for (TeacherClassSubject tcs : tcsList) {
                        int curWeekly = effectiveWeeklyByTcs.get(tcs.getId());
                        if (curWeekly >= 4) {
                            continue;
                        }
                        int added = addedByTcs.getOrDefault(tcs.getId(), 0);
                        int tLoad = teacherLessonCount.getOrDefault(tcs.getTeacher().getId(), 0) + curWeekly;
                        if (added < minAdded
                                || (added == minAdded && curWeekly > bestCurWeekly)
                                || (added == minAdded && curWeekly == bestCurWeekly && tLoad < minTeacherLoad)) {
                            minAdded = added;
                            bestCurWeekly = curWeekly;
                            minTeacherLoad = tLoad;
                            bestTcs = tcs;
                        }
                    }

                    // Fallback caso todas as disciplinas já tenham atingido 4 aulas/semana
                    if (bestTcs == null) {
                        int minWeekly = Integer.MAX_VALUE;
                        for (TeacherClassSubject tcs : tcsList) {
                            int curWeekly = effectiveWeeklyByTcs.get(tcs.getId());
                            int tLoad = teacherLessonCount.getOrDefault(tcs.getTeacher().getId(), 0) + curWeekly;
                            if (curWeekly < minWeekly || (curWeekly == minWeekly && tLoad < minTeacherLoad)) {
                                minWeekly = curWeekly;
                                minTeacherLoad = tLoad;
                                bestTcs = tcs;
                            }
                        }
                    }

                    if (bestTcs == null) {
                        break;
                    }
                    effectiveWeeklyByTcs.put(bestTcs.getId(), effectiveWeeklyByTcs.get(bestTcs.getId()) + 1);
                    addedByTcs.put(bestTcs.getId(), addedByTcs.getOrDefault(bestTcs.getId(), 0) + 1);
                    classroomNeeded++;
                }
            }

            if (classroomNeeded > maxAvailable) {
                throw new IllegalStateException(String.format(
                        "A turma '%s' possui %d aulas semanais atribuídas, mas a grade semanal comporta no máximo %d aulas (%d aulas/dia). Habilite a 6ª aula para a turma ou ajuste as matérias vinculadas.",
                        cr.getName(), classroomNeeded, maxAvailable, slotsPerDay));
            }

            for (TeacherClassSubject tcs : tcsList) {
                int weekly = effectiveWeeklyByTcs.get(tcs.getId());
                if (weekly > 0) {
                    teacherLessonCount.put(tcs.getTeacher().getId(),
                            teacherLessonCount.getOrDefault(tcs.getTeacher().getId(), 0) + weekly);
                    allGroups.add(new TcsGroup(groupSeq++, tcs, cr, tcs.getTeacher(), tcs.getSubject(), weekly));
                }
            }
        }

        if (allGroups.isEmpty()) {
            throw new IllegalStateException("Nenhuma matéria vinculada foi encontrada para as turmas selecionadas.");
        }

        int[] remainingByGroup = new int[allGroups.size()];
        int totalUnitsNeeded = 0;
        for (int i = 0; i < allGroups.size(); i++) {
            remainingByGroup[i] = allGroups.get(i).totalUnits();
            totalUnitsNeeded += remainingByGroup[i];
        }

        Map<ClassRoomSlotKey, TcsGroup> assignedSlots = new LinkedHashMap<>();
        Map<Integer, List<TimeInterval>> teacherIntervals = new HashMap<>();
        Map<ClassRoomDaySubjectKey, Integer> daySubjectCounts = new HashMap<>();
        Map<ClassRoomDaySubjectKey, List<Integer>> daySubjectSlotOrders = new HashMap<>();
        int[] stepCounter = new int[]{0};

        List<Integer> targetClassroomIds = targetClassrooms.stream().map(ClassRoom::getId).toList();

        // Carregar horários já existentes de outras turmas no mesmo ano letivo para evitar choques com os professores
        if (!targetClassroomIds.isEmpty()) {
            List<TimetableSlot> existingOtherSlots = timetableSlotRepository
                    .findByAcademicYearIdAndClassRoomIdNotIn(academicYear.getId(), targetClassroomIds);
            for (TimetableSlot otherSlot : existingOtherSlots) {
                if (otherSlot.getTeacherClassSubject() != null && otherSlot.getTeacherClassSubject().getTeacher() != null) {
                    Integer teacherId = otherSlot.getTeacherClassSubject().getTeacher().getId();
                    teacherIntervals.computeIfAbsent(teacherId, k -> new ArrayList<>())
                            .add(new TimeInterval(otherSlot.getDayOfWeek(), otherSlot.getStartTime(), otherSlot.getEndTime(), otherSlot.getClassRoom().getId()));
                }
            }
        }

        boolean solved = solveTimetable(
                0,
                totalUnitsNeeded,
                allGroups,
                remainingByGroup,
                teacherLessonCount,
                classroomSlots,
                assignedSlots,
                teacherIntervals,
                daySubjectCounts,
                daySubjectSlotOrders,
                System.currentTimeMillis(),
                stepCounter
        );

        if (!solved) {
            throw new ConflictException(
                    "Não foi possível gerar a grade horária automaticamente sem choque de horários para os professores com as restrições atuais. Verifique a carga e horários dos docentes."
            );
        }

        timetableSlotRepository.deleteByClassRoom_IdIn(targetClassroomIds);
        timetableSlotRepository.flush();

        List<TimetableSlot> slotsToSave = new ArrayList<>();
        for (Map.Entry<ClassRoomSlotKey, TcsGroup> entry : assignedSlots.entrySet()) {
            ClassRoomSlotKey key = entry.getKey();
            TcsGroup group = entry.getValue();

            SlotDefinition def = classroomSlots.get(group.classRoom()).stream()
                    .filter(s -> s.dayOfWeek() == key.dayOfWeek() && s.slotOrder() == key.slotOrder())
                    .findFirst()
                    .orElseThrow();

            TimetableSlot slot = TimetableSlot.builder()
                    .classRoom(group.classRoom())
                    .teacherClassSubject(group.tcs())
                    .dayOfWeek(def.dayOfWeek())
                    .startTime(def.startTime())
                    .endTime(def.endTime())
                    .slotOrder(def.slotOrder())
                    .build();

            slotsToSave.add(slot);
        }

        timetableSlotRepository.saveAll(slotsToSave);
        timetableSlotRepository.flush();

        log.info("Timetable auto-generated: {} slots saved for {} classrooms (search steps: {})",
                slotsToSave.size(), targetClassrooms.size(), stepCounter[0]);

        return new TimetableGenerationResponseDTO(
                slotsToSave.size(),
                targetClassrooms.size(),
                String.format("Grade horária gerada com sucesso! %d períodos de aula alocados em %d turma(s) sem choques de horários.",
                        slotsToSave.size(), targetClassrooms.size())
        );
    }

    private boolean solveTimetable(
            int placedCount,
            int totalUnitsNeeded,
            List<TcsGroup> groups,
            int[] remainingByGroup,
            Map<Integer, Integer> teacherLessonCount,
            Map<ClassRoom, List<SlotDefinition>> classroomSlots,
            Map<ClassRoomSlotKey, TcsGroup> assignedSlots,
            Map<Integer, List<TimeInterval>> teacherIntervals,
            Map<ClassRoomDaySubjectKey, Integer> daySubjectCounts,
            Map<ClassRoomDaySubjectKey, List<Integer>> daySubjectSlotOrders,
            long startTimeMillis,
            int[] stepCounter
    ) {
        if (placedCount >= totalUnitsNeeded) {
            return true;
        }

        stepCounter[0]++;
        if (stepCounter[0] > 150000 || (System.currentTimeMillis() - startTimeMillis) > 9000) {
            return false;
        }

        // 1. Calcular demanda restante por (turma, professor) e por turma
        Map<Long, Integer> classTeacherRem = new HashMap<>();
        Map<Integer, Integer> classTotalRem = new HashMap<>();
        for (int i = 0; i < groups.size(); i++) {
            int rem = remainingByGroup[i];
            if (rem <= 0) {
                continue;
            }
            TcsGroup g = groups.get(i);
            Integer crId = g.classRoom().getId();
            Integer tId = g.teacher().getId();
            long ctKey = (((long) crId) << 32) | (tId & 0xFFFFFFFFL);
            classTeacherRem.put(ctKey, classTeacherRem.getOrDefault(ctKey, 0) + rem);
            classTotalRem.put(crId, classTotalRem.getOrDefault(crId, 0) + rem);
        }

        // 2. Forward Checking e MRV por (Turma, Professor) / Grupo
        int bestGroupIdx = -1;
        int minGroupSlack = Integer.MAX_VALUE;
        int bestTeacherLoad = -1;
        int bestRemaining = -1;
        Map<Long, Integer> classTeacherValidCache = new HashMap<>();

        for (int i = 0; i < groups.size(); i++) {
            int rem = remainingByGroup[i];
            if (rem <= 0) {
                continue;
            }
            TcsGroup g = groups.get(i);
            Integer crId = g.classRoom().getId();
            Integer tId = g.teacher().getId();
            long ctKey = (((long) crId) << 32) | (tId & 0xFFFFFFFFL);

            int validCount = classTeacherValidCache.computeIfAbsent(ctKey,
                    k -> countConflictFreeSlots(g, classroomSlots.get(g.classRoom()), assignedSlots, teacherIntervals));
            int combinedRem = classTeacherRem.get(ctKey);

            // Se o professor não possui horários livres suficientes na turma para todas as suas disciplinas nela, podar imediatamente
            if (validCount < combinedRem) {
                return false;
            }

            int slack = validCount - combinedRem;
            int tLoad = teacherLessonCount.getOrDefault(tId, 0);

            if (slack < minGroupSlack
                    || (slack == minGroupSlack && tLoad > bestTeacherLoad)
                    || (slack == minGroupSlack && tLoad == bestTeacherLoad && rem > bestRemaining)) {
                minGroupSlack = slack;
                bestTeacherLoad = tLoad;
                bestRemaining = rem;
                bestGroupIdx = i;
            }
        }

        if (bestGroupIdx < 0) {
            return false;
        }

        // 3. Forward Checking e MRV por Slot de Turma (evita deixar um horário da turma sem nenhum professor disponível)
        ClassRoom bestSlotClassRoom = null;
        SlotDefinition bestSlotDef = null;
        int minSlotSlack = Integer.MAX_VALUE;

        for (Map.Entry<ClassRoom, List<SlotDefinition>> entry : classroomSlots.entrySet()) {
            ClassRoom cr = entry.getKey();
            Integer crId = cr.getId();
            int neededInCr = classTotalRem.getOrDefault(crId, 0);
            if (neededInCr <= 0) {
                continue;
            }

            List<SlotDefinition> crSlots = entry.getValue();
            int emptySlotsCount = 0;
            int usableEmptySlotsCount = 0;

            for (SlotDefinition slot : crSlots) {
                ClassRoomSlotKey slotKey = new ClassRoomSlotKey(crId, slot.dayOfWeek(), slot.slotOrder());
                if (!assignedSlots.containsKey(slotKey)) {
                    emptySlotsCount++;
                }
            }

            if (emptySlotsCount < neededInCr) {
                return false;
            }

            boolean mustFillEverySlot = (emptySlotsCount == neededInCr);

            for (SlotDefinition slot : crSlots) {
                ClassRoomSlotKey slotKey = new ClassRoomSlotKey(crId, slot.dayOfWeek(), slot.slotOrder());
                if (assignedSlots.containsKey(slotKey)) {
                    continue;
                }

                int eligibleGroups = countEligibleGroupsForSlot(crId, slot, groups, remainingByGroup, teacherIntervals);
                if (eligibleGroups > 0) {
                    usableEmptySlotsCount++;
                } else if (mustFillEverySlot) {
                    // Slot obrigatório sem nenhum professor disponível -> beco sem saída!
                    return false;
                }

                if (mustFillEverySlot) {
                    int slotSlack = eligibleGroups - 1;
                    if (slotSlack < minSlotSlack) {
                        minSlotSlack = slotSlack;
                        bestSlotClassRoom = cr;
                        bestSlotDef = slot;
                    }
                }
            }

            if (usableEmptySlotsCount < neededInCr) {
                return false;
            }
        }

        // 4. Decisão Dual-MRV: se há um slot obrigatório mais restrito que o grupo mais restrito, ramificar no slot
        if (bestSlotDef != null && minSlotSlack < minGroupSlack) {
            List<Integer> candidateGroupIndices = getOrderedGroupsForSlot(
                    bestSlotClassRoom.getId(),
                    bestSlotDef,
                    groups,
                    remainingByGroup,
                    teacherLessonCount,
                    teacherIntervals,
                    daySubjectCounts,
                    daySubjectSlotOrders,
                    false
            );
            if (candidateGroupIndices.isEmpty()) {
                candidateGroupIndices = getOrderedGroupsForSlot(
                        bestSlotClassRoom.getId(),
                        bestSlotDef,
                        groups,
                        remainingByGroup,
                        teacherLessonCount,
                        teacherIntervals,
                        daySubjectCounts,
                        daySubjectSlotOrders,
                        true
                );
            }

            ClassRoomSlotKey slotKey = new ClassRoomSlotKey(bestSlotClassRoom.getId(), bestSlotDef.dayOfWeek(), bestSlotDef.slotOrder());
            TimeInterval interval = new TimeInterval(bestSlotDef.dayOfWeek(), bestSlotDef.startTime(), bestSlotDef.endTime(), bestSlotClassRoom.getId());

            for (int gIdx : candidateGroupIndices) {
                TcsGroup g = groups.get(gIdx);
                ClassRoomDaySubjectKey dsKey = new ClassRoomDaySubjectKey(bestSlotClassRoom.getId(), bestSlotDef.dayOfWeek(), g.subject().getId());

                remainingByGroup[gIdx]--;
                assignedSlots.put(slotKey, g);
                teacherIntervals.computeIfAbsent(g.teacher().getId(), k -> new ArrayList<>()).add(interval);
                daySubjectCounts.put(dsKey, daySubjectCounts.getOrDefault(dsKey, 0) + 1);
                daySubjectSlotOrders.computeIfAbsent(dsKey, k -> new ArrayList<>()).add(bestSlotDef.slotOrder());

                if (solveTimetable(
                        placedCount + 1,
                        totalUnitsNeeded,
                        groups,
                        remainingByGroup,
                        teacherLessonCount,
                        classroomSlots,
                        assignedSlots,
                        teacherIntervals,
                        daySubjectCounts,
                        daySubjectSlotOrders,
                        startTimeMillis,
                        stepCounter)) {
                    return true;
                }

                remainingByGroup[gIdx]++;
                assignedSlots.remove(slotKey);
                teacherIntervals.get(g.teacher().getId()).remove(interval);
                int newCount = daySubjectCounts.get(dsKey) - 1;
                if (newCount <= 0) {
                    daySubjectCounts.remove(dsKey);
                } else {
                    daySubjectCounts.put(dsKey, newCount);
                }
                daySubjectSlotOrders.get(dsKey).remove((Integer) bestSlotDef.slotOrder());
            }

            return false;
        }

        // 5. Ramificar no grupo mais restrito (bestGroupIdx)
        TcsGroup group = groups.get(bestGroupIdx);
        ClassRoom cr = group.classRoom();
        List<SlotDefinition> availableSlots = classroomSlots.get(cr);

        List<SlotCandidate> candidates = getOrderedCandidates(
                group, availableSlots, assignedSlots, teacherIntervals, daySubjectCounts, daySubjectSlotOrders, false
        );

        if (candidates.isEmpty()) {
            candidates = getOrderedCandidates(
                    group, availableSlots, assignedSlots, teacherIntervals, daySubjectCounts, daySubjectSlotOrders, true
            );
        }

        for (SlotCandidate candidate : candidates) {
            SlotDefinition slot = candidate.slot();
            ClassRoomSlotKey slotKey = new ClassRoomSlotKey(cr.getId(), slot.dayOfWeek(), slot.slotOrder());
            TimeInterval interval = new TimeInterval(slot.dayOfWeek(), slot.startTime(), slot.endTime(), cr.getId());
            ClassRoomDaySubjectKey dsKey = new ClassRoomDaySubjectKey(cr.getId(), slot.dayOfWeek(), group.subject().getId());

            remainingByGroup[bestGroupIdx]--;
            assignedSlots.put(slotKey, group);
            teacherIntervals.computeIfAbsent(group.teacher().getId(), k -> new ArrayList<>()).add(interval);
            daySubjectCounts.put(dsKey, daySubjectCounts.getOrDefault(dsKey, 0) + 1);
            daySubjectSlotOrders.computeIfAbsent(dsKey, k -> new ArrayList<>()).add(slot.slotOrder());

            if (solveTimetable(
                    placedCount + 1,
                    totalUnitsNeeded,
                    groups,
                    remainingByGroup,
                    teacherLessonCount,
                    classroomSlots,
                    assignedSlots,
                    teacherIntervals,
                    daySubjectCounts,
                    daySubjectSlotOrders,
                    startTimeMillis,
                    stepCounter)) {
                return true;
            }

            remainingByGroup[bestGroupIdx]++;
            assignedSlots.remove(slotKey);
            teacherIntervals.get(group.teacher().getId()).remove(interval);
            int newCount = daySubjectCounts.get(dsKey) - 1;
            if (newCount <= 0) {
                daySubjectCounts.remove(dsKey);
            } else {
                daySubjectCounts.put(dsKey, newCount);
            }
            daySubjectSlotOrders.get(dsKey).remove((Integer) slot.slotOrder());
        }

        return false;
    }

    private int countEligibleGroupsForSlot(
            Integer classRoomId,
            SlotDefinition slot,
            List<TcsGroup> groups,
            int[] remainingByGroup,
            Map<Integer, List<TimeInterval>> teacherIntervals
    ) {
        int count = 0;
        for (int i = 0; i < groups.size(); i++) {
            if (remainingByGroup[i] <= 0) {
                continue;
            }
            TcsGroup g = groups.get(i);
            if (!g.classRoom().getId().equals(classRoomId)) {
                continue;
            }
            List<TimeInterval> tIntervals = teacherIntervals.getOrDefault(g.teacher().getId(), List.of());
            boolean conflict = false;
            for (TimeInterval ti : tIntervals) {
                if (ti.dayOfWeek() == slot.dayOfWeek()
                        && slot.startTime().isBefore(ti.endTime())
                        && ti.startTime().isBefore(slot.endTime())) {
                    conflict = true;
                    break;
                }
            }
            if (!conflict) {
                count++;
            }
        }
        return count;
    }

    private List<Integer> getOrderedGroupsForSlot(
            Integer classRoomId,
            SlotDefinition slot,
            List<TcsGroup> groups,
            int[] remainingByGroup,
            Map<Integer, Integer> teacherLessonCount,
            Map<Integer, List<TimeInterval>> teacherIntervals,
            Map<ClassRoomDaySubjectKey, Integer> daySubjectCounts,
            Map<ClassRoomDaySubjectKey, List<Integer>> daySubjectSlotOrders,
            boolean allowMoreThanTwoPerDay
    ) {
        record GroupCandidate(int groupIdx, int score) {}
        List<GroupCandidate> candidates = new ArrayList<>();

        for (int i = 0; i < groups.size(); i++) {
            int rem = remainingByGroup[i];
            if (rem <= 0) {
                continue;
            }
            TcsGroup g = groups.get(i);
            if (!g.classRoom().getId().equals(classRoomId)) {
                continue;
            }
            List<TimeInterval> tIntervals = teacherIntervals.getOrDefault(g.teacher().getId(), List.of());
            boolean conflict = false;
            for (TimeInterval ti : tIntervals) {
                if (ti.dayOfWeek() == slot.dayOfWeek()
                        && slot.startTime().isBefore(ti.endTime())
                        && ti.startTime().isBefore(slot.endTime())) {
                    conflict = true;
                    break;
                }
            }
            if (conflict) {
                continue;
            }

            ClassRoomDaySubjectKey dsKey = new ClassRoomDaySubjectKey(classRoomId, slot.dayOfWeek(), g.subject().getId());
            int currentDaySubjectCount = daySubjectCounts.getOrDefault(dsKey, 0);
            if (!allowMoreThanTwoPerDay && currentDaySubjectCount >= 2) {
                continue;
            }

            int score = rem * 50 + teacherLessonCount.getOrDefault(g.teacher().getId(), 0) * 5;
            if (currentDaySubjectCount == 0) {
                score += 1000;
            } else if (currentDaySubjectCount == 1) {
                List<Integer> existingOrders = daySubjectSlotOrders.getOrDefault(dsKey, List.of());
                boolean adjacentSameShift = false;
                boolean sameShift = false;
                for (Integer exOrder : existingOrders) {
                    if ((exOrder <= 5) == (slot.slotOrder() <= 5)) {
                        sameShift = true;
                        if (Math.abs(exOrder - slot.slotOrder()) == 1) {
                            adjacentSameShift = true;
                            break;
                        }
                    }
                }
                if (adjacentSameShift) {
                    score += 600;
                } else if (sameShift) {
                    score += 100;
                } else {
                    score -= 400;
                }
            }

            candidates.add(new GroupCandidate(i, score));
        }

        candidates.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return candidates.stream().map(GroupCandidate::groupIdx).toList();
    }

    private int countConflictFreeSlots(
            TcsGroup group,
            List<SlotDefinition> slots,
            Map<ClassRoomSlotKey, TcsGroup> assignedSlots,
            Map<Integer, List<TimeInterval>> teacherIntervals
    ) {
        Integer classRoomId = group.classRoom().getId();
        Integer teacherId = group.teacher().getId();
        List<TimeInterval> tIntervals = teacherIntervals.getOrDefault(teacherId, List.of());
        int count = 0;

        for (SlotDefinition slot : slots) {
            ClassRoomSlotKey slotKey = new ClassRoomSlotKey(classRoomId, slot.dayOfWeek(), slot.slotOrder());
            if (assignedSlots.containsKey(slotKey)) {
                continue;
            }

            boolean teacherConflict = false;
            for (TimeInterval ti : tIntervals) {
                if (ti.dayOfWeek() == slot.dayOfWeek()
                        && slot.startTime().isBefore(ti.endTime())
                        && ti.startTime().isBefore(slot.endTime())) {
                    teacherConflict = true;
                    break;
                }
            }
            if (!teacherConflict) {
                count++;
            }
        }
        return count;
    }

    private List<SlotCandidate> getOrderedCandidates(
            TcsGroup group,
            List<SlotDefinition> slots,
            Map<ClassRoomSlotKey, TcsGroup> assignedSlots,
            Map<Integer, List<TimeInterval>> teacherIntervals,
            Map<ClassRoomDaySubjectKey, Integer> daySubjectCounts,
            Map<ClassRoomDaySubjectKey, List<Integer>> daySubjectSlotOrders,
            boolean allowMoreThanTwoPerDay
    ) {
        List<SlotCandidate> candidates = new ArrayList<>();
        Integer classRoomId = group.classRoom().getId();
        Integer teacherId = group.teacher().getId();
        Integer subjectId = group.subject().getId();
        List<TimeInterval> tIntervals = teacherIntervals.getOrDefault(teacherId, List.of());

        for (SlotDefinition slot : slots) {
            ClassRoomSlotKey slotKey = new ClassRoomSlotKey(classRoomId, slot.dayOfWeek(), slot.slotOrder());
            if (assignedSlots.containsKey(slotKey)) {
                continue;
            }

            boolean teacherConflict = false;
            for (TimeInterval ti : tIntervals) {
                if (ti.dayOfWeek() == slot.dayOfWeek()
                        && slot.startTime().isBefore(ti.endTime())
                        && ti.startTime().isBefore(slot.endTime())) {
                    teacherConflict = true;
                    break;
                }
            }
            if (teacherConflict) {
                continue;
            }

            ClassRoomDaySubjectKey dsKey = new ClassRoomDaySubjectKey(classRoomId, slot.dayOfWeek(), subjectId);
            int currentDaySubjectCount = daySubjectCounts.getOrDefault(dsKey, 0);
            if (!allowMoreThanTwoPerDay && currentDaySubjectCount >= 2) {
                continue;
            }

            int score = 0;
            if (currentDaySubjectCount == 0) {
                // Priorizar distribuir a matéria em dias diferentes da semana
                score += 1000;
            } else if (currentDaySubjectCount == 1) {
                List<Integer> existingOrders = daySubjectSlotOrders.getOrDefault(dsKey, List.of());
                boolean adjacentSameShift = false;
                boolean sameShift = false;
                for (Integer exOrder : existingOrders) {
                    if ((exOrder <= 5) == (slot.slotOrder() <= 5)) {
                        sameShift = true;
                        if (Math.abs(exOrder - slot.slotOrder()) == 1) {
                            adjacentSameShift = true;
                            break;
                        }
                    }
                }
                if (adjacentSameShift) {
                    score += 600;
                } else if (sameShift) {
                    score += 100;
                } else {
                    score -= 400;
                }
            }

            // Desempate rotacionado por turma para evitar que todas as turmas deixem o mesmo horário por último
            int rotatedPenalty = ((slot.slotOrder() + (classRoomId != null ? classRoomId * 3 : 0) + slot.dayOfWeek().ordinal() * 2) % 10) * 8;
            score -= rotatedPenalty;

            candidates.add(new SlotCandidate(slot, score));
        }

        candidates.sort((a, b) -> Integer.compare(b.score(), a.score()));
        return candidates;
    }

    private List<SlotTime> computeShiftSlotTimes(
            java.time.LocalTime start,
            java.time.LocalTime end,
            int numLessons,
            int breakMinutes,
            Integer explicitDuration,
            boolean isNight
    ) {
        List<SlotTime> result = new ArrayList<>();
        int baseLessons = (!isNight && end != null && java.time.Duration.between(start, end).toMinutes() >= 260) ? 6 : 5;

        if (end == null) {
            int dur = explicitDuration != null ? explicitDuration : 45;
            java.time.LocalTime cur = start;
            for (int i = 1; i <= numLessons; i++) {
                if (i == 4 && breakMinutes > 0) {
                    cur = cur.plusMinutes(breakMinutes);
                }
                java.time.LocalTime next = cur.plusMinutes(dur);
                result.add(new SlotTime(cur, next));
                cur = next;
            }
            return result;
        }

        int totalMinutes = (int) java.time.Duration.between(start, end).toMinutes();
        int netMinutes = totalMinutes - breakMinutes;
        if (netMinutes <= 0) {
            throw new IllegalArgumentException(String.format(
                    "O horário de término (%s) deve permitir ao menos o intervalo de %d minutos a partir do início (%s).",
                    end, breakMinutes, start));
        }

        // Duração padrão arredondada para múltiplo de 5 minutos (horários redondos: ex: 45 min)
        int rawAverage = (int) Math.round((double) netMinutes / baseLessons);
        int roundedDuration = (int) (Math.round((double) rawAverage / 5.0) * 5);
        if (roundedDuration < 30) {
            roundedDuration = 30;
        }

        // Calcula a 4ª aula ajustada para absorver a diferença com o horário final
        // netMinutes = (baseLessons - 1) * roundedDuration + fourthDuration
        int fourthDuration = netMinutes - ((baseLessons - 1) * roundedDuration);

        // Se a 4ª aula ficou menor que 30 min ou não for múltiplo de 5, busca múltiplos de 5 redondos
        if (fourthDuration < 20 || fourthDuration > 60 || fourthDuration % 5 != 0) {
            // Se não der exato com baseLessons, ajusta mantendo todos os horários terminando em :x0 ou :x5
            // Prioriza duração redonda padrão de 45 ou 40 ou 50 min
            int bestStandard = 45;
            for (int candidate : new int[]{45, 40, 50, 35, 30}) {
                int rem = netMinutes - ((baseLessons - 1) * candidate);
                if (rem >= 25 && rem <= 60 && rem % 5 == 0) {
                    roundedDuration = candidate;
                    fourthDuration = rem;
                    break;
                }
            }
        }

        java.time.LocalTime cur = start;
        for (int i = 1; i <= Math.min(numLessons, baseLessons); i++) {
            if (i == 4 && breakMinutes > 0) {
                cur = cur.plusMinutes(breakMinutes);
            }
            int dur = (i == 4) ? fourthDuration : roundedDuration;
            java.time.LocalTime next = cur.plusMinutes(dur);
            result.add(new SlotTime(cur, next));
            cur = next;
        }

        if (numLessons == 6 && baseLessons == 5) {
            java.time.LocalTime next = cur.plusMinutes(roundedDuration);
            result.add(new SlotTime(cur, next));
        }

        return result;
    }

    private void validateGenerationRequest(TimetableGenerationRequestDTO dto) {
        if (dto.lessonDurationMinutes() != null && (dto.lessonDurationMinutes() < 30 || dto.lessonDurationMinutes() > 120)) {
            throw new IllegalArgumentException("A duração da aula deve ser entre 30 e 120 minutos.");
        }
        java.time.LocalTime morningStart = dto.morningStartTime() != null ? dto.morningStartTime() : dto.startTime();
        if (morningStart == null && dto.startTime() == null) {
            throw new IllegalArgumentException("O horário de início das aulas é obrigatório.");
        }
        if (dto.breakDurationMinutes() != null && (dto.breakDurationMinutes() < 0 || dto.breakDurationMinutes() > 60)) {
            throw new IllegalArgumentException("A duração do intervalo deve ser entre 0 e 60 minutos.");
        }
        if (dto.morningStartTime() != null && dto.morningEndTime() != null && !dto.morningEndTime().isAfter(dto.morningStartTime())) {
            throw new IllegalArgumentException("O horário de saída da manhã deve ser posterior ao horário de entrada.");
        }
        if (dto.afternoonStartTime() != null && dto.afternoonEndTime() != null && !dto.afternoonEndTime().isAfter(dto.afternoonStartTime())) {
            throw new IllegalArgumentException("O horário de saída da tarde deve ser posterior ao horário de entrada.");
        }
        if (dto.nightStartTime() != null && dto.nightEndTime() != null && !dto.nightEndTime().isAfter(dto.nightStartTime())) {
            throw new IllegalArgumentException("O horário de saída da noite deve ser posterior ao horário de entrada.");
        }
    }

    private AcademicYear resolveAcademicYear(Integer academicYearId) {
        if (academicYearId != null) {
            return academicYearRepository.findById(academicYearId)
                    .orElseThrow(() -> new EntityNotFoundException("Ano letivo não encontrado com o ID: " + academicYearId));
        }
        return academicYearRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Não há ano letivo ativo cadastrado no sistema."));
    }

    private List<ClassRoom> resolveTargetClassrooms(AcademicYear academicYear, List<Integer> classRoomIds) {
        if (classRoomIds != null && !classRoomIds.isEmpty()) {
            return classRoomRepository.findAllById(classRoomIds).stream()
                    .filter(ClassRoom::isActive)
                    .filter(c -> c.getAcademicYear() != null && c.getAcademicYear().getId().equals(academicYear.getId()))
                    .toList();
        }
        return classRoomRepository.findByAcademicYearAndActiveTrue(academicYear).stream()
                .filter(c -> !teacherClassSubjectRepository.findByClassRoomAndActiveTrue(c).isEmpty())
                .toList();
    }
}
