package com.andretti101.escolaweb.service.impl;

import com.andretti101.escolaweb.model.entity.AcademicYear;
import com.andretti101.escolaweb.model.entity.ClassRoom;
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
    private final EnrollmentRepository enrollmentRepository;
    private final TeacherClassSubjectRepository teacherClassSubjectRepository;
    private final AcademicYearService academicYearService;

    @Override
    @Transactional
    public ClassRoom create(ClassRoom classRoom) {
        AcademicYear year = resolveAcademicYear(classRoom.getAcademicYear());
        classRoom.setAcademicYear(year);

        if (classRoomRepository.existsByAcademicYearAndName(year, classRoom.getName())) {
            throw new IllegalStateException(
                    "Classroom '" + classRoom.getName() + "' already exists in academic year " + year.getYear());
        }

        classRoom.setActive(true);
        return classRoom;
    }

    @Override
    @Transactional
    public ClassRoom update(Integer id, ClassRoom incoming) {
        ClassRoom existing = findClassRoomOrThrow(id);
        AcademicYear year = resolveAcademicYear(incoming.getAcademicYear());

        boolean nameChanged = !existing.getName().equals(incoming.getName());
        boolean yearChanged = !existing.getAcademicYear().getId().equals(year.getId());

        if ((nameChanged || yearChanged)
                && classRoomRepository.existsByAcademicYearAndName(year, incoming.getName())) {
            throw new IllegalStateException(
                    "Classroom '" + incoming.getName() + "' already exists in academic year " + year.getYear());
        }

        existing.setName(incoming.getName());
        existing.setShift(incoming.getShift());
        existing.setCreationYear(incoming.getCreationYear());
        existing.setAcademicYear(year);

        return classRoomRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        ClassRoom classRoom = findClassRoomOrThrow(id);

        if (enrollmentRepository.existsByClassRoom(classRoom)) {
            throw new IllegalStateException(
                    "Cannot delete classroom with id " + id + " because it has student enrollments.");
        }
        if (teacherClassSubjectRepository.existsByClassRoom(classRoom)) {
            throw new IllegalStateException(
                    "Cannot delete classroom with id " + id + " because it has teacher-subject assignments.");
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
    public List<ClassRoom> findAllActive() {
        return classRoomRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRoom> findByAcademicYear(Integer academicYearId) {
        AcademicYear year = academicYearService.findById(academicYearId);
        return classRoomRepository.findByAcademicYear(year);
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
                .orElseThrow(() -> new EntityNotFoundException("Classroom not found with id: " + id));
    }

    private AcademicYear resolveAcademicYear(AcademicYear ref) {
        if (ref == null || ref.getId() == null) {
            throw new IllegalArgumentException("Academic year is required.");
        }
        return academicYearService.findById(ref.getId());
    }
}
