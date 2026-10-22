package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.ClassRoom;
import com.andretti101.escolaweb.repository.AcademicPeriodRepository;
import com.andretti101.escolaweb.repository.ClassRoomRepository;
import com.andretti101.escolaweb.repository.EnrollmentRepository;
import com.andretti101.escolaweb.repository.TeacherClassSubjectRepository;
import com.andretti101.escolaweb.service.AcademicYearService;
import com.andretti101.escolaweb.service.ClassRoomService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClassRoomServiceImpl implements ClassRoomService {

    private final ClassRoomRepository classRoomRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final AcademicYearService academicYearService;
    private final com.andretti101.escolaweb.repository.AcademicYearRepository academicYearRepository;

    // Cria uma nova turma validando obrigatoriedade da série, existência prévia dos períodos e unicidade
    @Override
    @Transactional
    public ClassRoom create(ClassRoom classRoom) {
        AcademicYear year = resolveAcademicYear(classRoom.getAcademicYear());
        classRoom.setAcademicYear(year);

        if (classRoom.getPeriodType() == null) {
            throw new IllegalArgumentException("O tipo de divisão da turma é obrigatório.");
        }

        if (classRoom.getSchoolGrade() == null) {
            throw new IllegalArgumentException("A série da turma é obrigatória.");
        }

        // A direção precisa ter configurado a divisão (Bimestre/Trimestre) no ano antes de criar turmas daquele modelo
        if (!academicPeriodRepository.existsByAcademicYearAndPeriodType(year, classRoom.getPeriodType())) {
            throw new IllegalStateException("A direção ainda não criou a divisão de "
                    + classRoom.getPeriodType().getLabel() + " para o ano letivo " + year.getYear() + ".");
        }

        // Unicidade do nome da turma dentro do mesmo ano letivo
        if (classRoomRepository.existsByAcademicYearAndName(year, classRoom.getName())) {
            throw new IllegalStateException(
                    "Já existe uma turma com o nome '" + classRoom.getName() + "' no ano letivo " + year.getYear() + ".");
        }

        classRoom.setActive(true);
        return classRoomRepository.save(classRoom);
    }

    @Override
    @Transactional
    public ClassRoom update(Integer id, ClassRoom incoming) {
        ClassRoom existing = findClassRoomOrThrow(id);
        AcademicYear year = resolveAcademicYear(incoming.getAcademicYear());

        if (incoming.getPeriodType() != null && incoming.getPeriodType() != existing.getPeriodType()) {
            throw new IllegalStateException("O tipo de divisão da turma não pode ser alterado.");
        }

        if (incoming.getSchoolGrade() == null) {
            throw new IllegalArgumentException("A série da turma é obrigatória.");
        }

        boolean nameChanged = !existing.getName().equals(incoming.getName());
        boolean yearChanged = !existing.getAcademicYear().getId().equals(year.getId());

        if (yearChanged) {
            if (!academicPeriodRepository.existsByAcademicYearAndPeriodType(year, existing.getPeriodType())) {
                throw new IllegalStateException("A direção ainda não criou a divisão "
                        + existing.getPeriodType().getLabel() + " para o ano letivo " + year.getYear()
                        + ". Crie a divisão antes de vincular a turma a este ano.");
            }
            if (enrollmentRepository.existsByClassRoom(existing)) {
                throw new IllegalStateException("Não é possível alterar o ano letivo da turma '"
                        + existing.getName() + "' pois ela já possui matrículas vinculadas.");
            }
            if (teacherClassSubjectRepository.existsByClassRoom(existing)) {
                throw new IllegalStateException("Não é possível alterar o ano letivo da turma '"
                        + existing.getName() + "' pois ela já possui disciplinas ou professores vinculados.");
            }
        }

        if ((nameChanged || yearChanged)
                && classRoomRepository.existsByAcademicYearAndName(year, incoming.getName())) {
            throw new IllegalStateException(
                    "Já existe uma turma com o nome '" + incoming.getName() + "' no ano letivo " + year.getYear() + ".");
        }

        existing.setName(incoming.getName());
        existing.setShift(incoming.getShift());
        existing.setSchoolGrade(incoming.getSchoolGrade());
        existing.setAcademicYear(year);

        return classRoomRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        ClassRoom classRoom = findClassRoomOrThrow(id);

        if (enrollmentRepository.existsByClassRoom(classRoom)) {
            throw new IllegalStateException(
                    "Não é possível excluir a turma '" + classRoom.getName() + "' pois ela possui matrículas de alunos vinculadas. Remova ou transfira as matrículas antes de excluir.");
        }
        if (teacherClassSubjectRepository.existsByClassRoom(classRoom)) {
            throw new IllegalStateException(
                    "Não é possível excluir a turma '" + classRoom.getName() + "' pois ela possui disciplinas e professores atribuídos. Remova as atribuições antes de excluir.");
        }

        classRoomRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassRoom findById(Integer id) {
        return findClassRoomOrThrow(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRoom> findAll() {
        return classRoomRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ClassRoom> findAll(org.springframework.data.domain.Pageable pageable) {
        return classRoomRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRoom> findAllActive() {
        return classRoomRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ClassRoom> findAllActive(org.springframework.data.domain.Pageable pageable) {
        return classRoomRepository.findByActiveTrue(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRoom> findByAcademicYear(Integer academicYearId) {
        if (academicYearId == null) return java.util.Collections.emptyList();
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseGet(() -> academicYearRepository.findByYear(academicYearId)
                        .orElseThrow(() -> new EntityNotFoundException("Ano letivo não encontrado.")));
        return classRoomRepository.findByAcademicYear(year);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ClassRoom> findByAcademicYear(Integer academicYearId, org.springframework.data.domain.Pageable pageable) {
        if (academicYearId == null) return org.springframework.data.domain.Page.empty(pageable);
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseGet(() -> academicYearRepository.findByYear(academicYearId)
                        .orElseThrow(() -> new EntityNotFoundException("Ano letivo não encontrado.")));
        return classRoomRepository.findByAcademicYear(year, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ClassRoom> findByAcademicYearAndActiveTrue(Integer academicYearId, org.springframework.data.domain.Pageable pageable) {
        if (academicYearId == null) return org.springframework.data.domain.Page.empty(pageable);
        AcademicYear year = academicYearRepository.findById(academicYearId)
                .orElseGet(() -> academicYearRepository.findByYear(academicYearId)
                        .orElseThrow(() -> new EntityNotFoundException("Ano letivo não encontrado.")));
        return classRoomRepository.findByAcademicYearAndActiveTrue(year, pageable);
    }

    @Override
    @Transactional
    public ClassRoom activate(Integer id) {
        ClassRoom classRoom = findClassRoomOrThrow(id);
        classRoom.setActive(true);
        return classRoom;
    }

    @Override
    @Transactional
    public ClassRoom deactivate(Integer id) {
        ClassRoom classRoom = findClassRoomOrThrow(id);
        classRoom.setActive(false);
        return classRoom;
    }

    ClassRoom findClassRoomOrThrow(Integer id) {
        return classRoomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Turma não encontrada."));
    }

    private AcademicYear resolveAcademicYear(AcademicYear ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("A seleção do ano letivo é obrigatória.");
        }
        return academicYearService.findById(ref.getId());
    }
}
