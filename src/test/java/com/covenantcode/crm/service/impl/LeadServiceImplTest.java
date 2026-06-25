package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.lead.*;
import com.covenantcode.crm.dto.lead.*;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.dto.lead.*;
import com.covenantcode.crm.entity.*;
import com.covenantcode.crm.entity.enums.LeadStatus;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.LeadComment;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.LeadCommentMapper;
import com.covenantcode.crm.mapper.LeadMapper;
import com.covenantcode.crm.mapper.StudentMapper;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.LeadCommentRepository;
import com.covenantcode.crm.repository.LeadRepository;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceImplTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentMapper studentMapper;

    @Mock
    private LeadMapper leadMapper;

    @Mock
    private LeadCommentRepository leadCommentRepository;

    @Mock
    private LeadCommentMapper leadCommentMapper;

    @InjectMocks
    private LeadServiceImpl leadService;

    private LeadCreateRequest fullRequest;
    private LeadCreateRequest minimalRequest;
    private Course course;
    private User manager;
    private Lead savedLead;
    private LeadResponse expectedResponse;
    private LeadCommentCreateRequest commentRequest;
    private Lead existingLead;
    private User author;
    private LeadComment savedComment;
    private LeadCommentResponse commentResponse;

    private Lead lead;
    private LeadResponse leadResponse;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(1L);
        course.setTitle("Test Course");

        manager = new User();
        manager.setId(2L);
        manager.setFirstName("Anna");
        manager.setLastName("Smith");

        fullRequest = new LeadCreateRequest();
        fullRequest.setFirstName("Ivan");
        fullRequest.setLastName("Petrov");
        fullRequest.setPhone("+79001234567");
        fullRequest.setEmail("ivan@example.com");
        fullRequest.setSource("website");
        fullRequest.setInterestedCourseId(1L);
        fullRequest.setAssignedManagerId(2L);
        fullRequest.setComment("Test comment");

        minimalRequest = new LeadCreateRequest();
        minimalRequest.setFirstName("John");
        minimalRequest.setPhone("+79998887766");

        savedLead = new Lead();
        savedLead.setId(1L);
        savedLead.setFirstName(fullRequest.getFirstName());
        savedLead.setLastName(fullRequest.getLastName());
        savedLead.setPhone(fullRequest.getPhone());
        savedLead.setEmail(fullRequest.getEmail());
        savedLead.setSource(fullRequest.getSource());
        savedLead.setComment(fullRequest.getComment());
        savedLead.setInterestedCourse(course);
        savedLead.setAssignedManager(manager);
        savedLead.setStatus(LeadStatus.NEW);

        expectedResponse = new LeadResponse();
        expectedResponse.setId(1L);
        expectedResponse.setFirstName(fullRequest.getFirstName());
        expectedResponse.setLastName(fullRequest.getLastName());
        expectedResponse.setPhone(fullRequest.getPhone());
        expectedResponse.setEmail(fullRequest.getEmail());
        expectedResponse.setSource(fullRequest.getSource());
        expectedResponse.setComment(fullRequest.getComment());
        expectedResponse.setStatus(LeadStatus.NEW.name());
        expectedResponse.setCreatedAt(LocalDateTime.now());
        expectedResponse.setUpdatedAt(LocalDateTime.now());

        CourseShortResponse courseShort = new CourseShortResponse();
        courseShort.setId(1L);
        courseShort.setTitle("Test Course");
        expectedResponse.setInterestedCourse(courseShort);

        UserShortResponse userShort = new UserShortResponse();
        userShort.setId(2L);
        userShort.setFirstName("Anna");
        userShort.setLastName("Smith");
        expectedResponse.setAssignedManager(userShort);

        lead = new Lead();
        lead.setId(1L);

        leadResponse = new LeadResponse();
        leadResponse.setId(1L);

        commentRequest = LeadCommentCreateRequest.builder()
                .text("Test comment text")
                .build();

        existingLead = new Lead();
        existingLead.setId(1L);
        existingLead.setFirstName("Ivan");
        existingLead.setLastName("Petrov");

        author = new User();
        author.setId(10L);
        author.setFirstName("Anna");
        author.setLastName("Smith");
        author.setEmail("anna@example.com");

        savedComment = LeadComment.builder()
                .id(100L)
                .lead(existingLead)
                .author(author)
                .text("Test comment text")
                .build();

        commentResponse = LeadCommentResponse.builder()
                .id(100L)
                .leadId(1L)
                .author(new UserShortResponse(10L, "Anna", "Smith"))
                .text("Test comment text")
                .createdAt(LocalDateTime.now())
                .build();
    }


    @Test
    @DisplayName("Создание лида с заполненными полями (курс и менеджер)")
    void createLead_withAllFields_shouldReturnLeadResponse() {

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(leadRepository.save(any(Lead.class))).thenReturn(savedLead);

        when(leadMapper.toResponse(any(Lead.class))).thenReturn(expectedResponse);

        LeadResponse response = leadService.create(fullRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("NEW");
        assertThat(response.getInterestedCourse()).isNotNull();
        assertThat(response.getInterestedCourse().getId()).isEqualTo(1L);

        verify(courseRepository).findById(1L);
        verify(userRepository).findById(2L);
        verify(leadRepository).save(any(Lead.class));
        verify(leadMapper).toResponse(any(Lead.class));

        ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(leadCaptor.capture());
        Lead capturedLead = leadCaptor.getValue();
        assertThat(capturedLead.getStatus()).isEqualTo(LeadStatus.NEW);
        assertThat(capturedLead.getInterestedCourse()).isEqualTo(course);
        assertThat(capturedLead.getAssignedManager()).isEqualTo(manager);
    }

    @Test
    @DisplayName("Создание лида только с обязательными полями (минимум)")
    void createLead_withMinimalFields_shouldReturnLeadResponse() {

        Lead minimalSavedLead = new Lead();
        minimalSavedLead.setId(2L);
        minimalSavedLead.setFirstName(minimalRequest.getFirstName());
        minimalSavedLead.setPhone(minimalRequest.getPhone());
        minimalSavedLead.setStatus(LeadStatus.NEW);

        LeadResponse minimalResponse = new LeadResponse();
        minimalResponse.setId(2L);
        minimalResponse.setFirstName(minimalRequest.getFirstName());
        minimalResponse.setPhone(minimalRequest.getPhone());
        minimalResponse.setStatus("NEW");
        minimalResponse.setInterestedCourse(null);
        minimalResponse.setAssignedManager(null);

        when(leadRepository.save(any(Lead.class))).thenReturn(minimalSavedLead);
        when(leadMapper.toResponse(any(Lead.class))).thenReturn(minimalResponse);

        LeadResponse response = leadService.create(minimalRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getStatus()).isEqualTo("NEW");
        assertThat(response.getInterestedCourse()).isNull();
        assertThat(response.getAssignedManager()).isNull();

        verify(courseRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
        verify(leadRepository).save(any(Lead.class));

        ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(leadCaptor.capture());
        Lead captured = leadCaptor.getValue();
        assertThat(captured.getStatus()).isEqualTo(LeadStatus.NEW);
        assertThat(captured.getInterestedCourse()).isNull();
        assertThat(captured.getAssignedManager()).isNull();
    }

    @Test
    @DisplayName("Передан несуществующий курс – выбрасывается ResourceNotFoundException")
    void createLead_withInvalidCourseId_shouldThrowResourceNotFoundException() {

        Long invalidCourseId = 99L;
        fullRequest.setInterestedCourseId(invalidCourseId);
        fullRequest.setAssignedManagerId(2L);

        when(courseRepository.findById(invalidCourseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.create(fullRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Course")
                .hasMessageContaining(String.valueOf(invalidCourseId));

        verify(courseRepository).findById(invalidCourseId);
        verify(userRepository, never()).findById(anyLong());
        verify(leadRepository, never()).save(any(Lead.class));
    }

    @Test
    void getById_whenLeadFound_thenReturnsLeadResponse() {
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));
        when(leadMapper.toResponse(lead)).thenReturn(leadResponse);

        LeadResponse result = leadService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(leadRepository, times(1)).findById(1L);
    }

    @Test
    void getById_whenLeadNotFound_thenThrowsException() {
        when(leadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> leadService.getById(99L));

        verify(leadRepository, times(1)).findById(99L);
        verifyNoInteractions(leadMapper);
    }

    // --- Тест 1: Без фильтров, возвращает все лиды ---
    @Test
    @DisplayName("Возвращает все лиды постранично без фильтров")
    void getAllWithoutFiltersShouldReturnAllLeadsPaginated() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Lead lead1 = new Lead();
        lead1.setId(1L);
        lead1.setFirstName("Lead1");

        Lead lead2 = new Lead();
        lead2.setId(2L);
        lead2.setFirstName("Lead2");

        List<Lead> leads = List.of(lead1, lead2);
        Page<Lead> leadPage = new PageImpl<>(leads, pageable, leads.size());

        LeadResponse leadResponse1 = new LeadResponse();
        leadResponse1.setId(1L);
        leadResponse1.setFirstName("Lead1");

        LeadResponse leadResponse2 = new LeadResponse();
        leadResponse2.setId(2L);
        leadResponse2.setFirstName("Lead2");

        // Mocking
        when(leadRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(leadPage);
        when(leadMapper.toResponse(lead1)).thenReturn(leadResponse1);
        when(leadMapper.toResponse(lead2)).thenReturn(leadResponse2);

        // When
        Page<LeadResponse> result = leadService.getAll(
                null,    // search
                null,    // status
                null,    // assignedManagerId
                null,    // interestedCourseId
                pageable
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    // --- Тест 2: С фильтром status=NEW ---
    @Test
    @DisplayName("Фильтрует лиды по статусу NEW, спецификация передаётся в репозиторий")
    void getAllWithStatusFilterShouldApplySpecification() {
        // Given
        LeadStatus status = LeadStatus.NEW;
        Pageable pageable = PageRequest.of(0, 20);

        Lead leadWithStatusNew = new Lead();
        leadWithStatusNew.setId(1L);
        leadWithStatusNew.setStatus(status);

        List<Lead> leads = List.of(leadWithStatusNew);
        Page<Lead> leadPage = new PageImpl<>(leads, pageable, leads.size());

        LeadResponse leadResponse = new LeadResponse();
        leadResponse.setId(1L);

        // Mocking
        when(leadRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(leadPage);
        when(leadMapper.toResponse(leadWithStatusNew)).thenReturn(leadResponse);

        // When
        Page<LeadResponse> result = leadService.getAll(
                null,      // search
                status,    // status=NEW
                null,      // assignedManagerId
                null,      // interestedCourseId
                pageable
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        // Проверка, что спецификация передаётся в репозиторий
        ArgumentCaptor<Specification<Lead>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(leadRepository, times(1)).findAll(specCaptor.capture(), eq(pageable));

        Specification<Lead> capturedSpec = specCaptor.getValue();
        assertThat(capturedSpec).isNotNull();
    }

    @Test
    @DisplayName("Успешная конвертация лида в студента")
    void convertToStudent_Success() {

        Long leadId = 1L;
        LeadConvertRequest request = LeadConvertRequest.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .phone("+79001112233")
                .email("ivan@test.com")
                .birthDate(LocalDate.of(2000, 1, 1))
                .build();

        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setStatus(LeadStatus.NEW);

        Student savedStudent = new Student();
        savedStudent.setId(10L);
        savedStudent.setFirstName(request.getFirstName());

        savedStudent.setUser(null);

        StudentResponse expectedResponse = new StudentResponse();
        expectedResponse.setId(10L);
        expectedResponse.setFirstName("Иван");

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(studentRepository.save(any(Student.class))).thenAnswer(invocation -> {
            Student s = invocation.getArgument(0);
            s.setId(10L);
            return s;
        });
        when(studentMapper.toResponse(any(Student.class))).thenReturn(expectedResponse);

        StudentResponse result = leadService.convertToStudent(leadId, request);

        assertNotNull(result);
        assertEquals("Иван", result.getFirstName());

        assertEquals(LeadStatus.CONVERTED_TO_STUDENT, lead.getStatus());

        assertNotNull(lead.getConvertedStudent());
        assertEquals(10L, lead.getConvertedStudent().getId());

        assertNull(lead.getConvertedStudent().getUser(), "UserId должен быть null");

        verify(studentRepository, times(1)).save(any(Student.class));

        verify(leadRepository, times(1)).save(lead);
    }

    @Test
    @DisplayName("Конвертация: лид не найден - ResourceNotFoundException")
    void convertToStudent_LeadNotFound_ThrowsException() {

        when(leadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> leadService.convertToStudent(99L, new LeadConvertRequest()));

        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Конвертация: лид уже конвертирован - ConflictException")
    void convertToStudent_AlreadyConverted_ThrowsException() {

        Lead lead = new Lead();
        lead.setStatus(LeadStatus.CONVERTED_TO_STUDENT);
        when(leadRepository.findById(1L)).thenReturn(Optional.of(lead));

        assertThrows(ConflictException.class,
                () -> leadService.convertToStudent(1L, new LeadConvertRequest()));

        verify(studentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успешное добавление комментария - лид найден, автор найден")
    void addComment_whenLeadAndAuthorFound_shouldReturnLeadCommentResponse() {
        Long leadId = 1L;
        Long authorId = 10L;

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(existingLead));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(leadCommentRepository.save(any(LeadComment.class))).thenReturn(savedComment);
        when(leadCommentMapper.toResponse(savedComment)).thenReturn(commentResponse);

        LeadCommentResponse result = leadService.addComment(leadId, commentRequest, authorId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getLeadId()).isEqualTo(1L);
        assertThat(result.getText()).isEqualTo("Test comment text");
        assertThat(result.getAuthor()).isNotNull();
        assertThat(result.getAuthor().getId()).isEqualTo(10L);
        assertThat(result.getAuthor().getFirstName()).isEqualTo("Anna");
        assertThat(result.getAuthor().getLastName()).isEqualTo("Smith");

        verify(leadRepository, times(1)).findById(leadId);
        verify(userRepository, times(1)).findById(authorId);
        verify(leadCommentRepository, times(1)).save(any(LeadComment.class));
        verify(leadCommentMapper, times(1)).toResponse(savedComment);
    }

    @Test
    @DisplayName("Лид не найден - бросается ResourceNotFoundException")
    void addComment_whenLeadNotFound_shouldThrowResourceNotFoundException() {
        Long leadId = 999L;
        Long authorId = 10L;

        when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.addComment(leadId, commentRequest, authorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Lead")
                .hasMessageContaining(String.valueOf(leadId));

        verify(leadRepository, times(1)).findById(leadId);
        verify(userRepository, never()).findById(anyLong());
        verify(leadCommentRepository, never()).save(any(LeadComment.class));
        verify(leadCommentMapper, never()).toResponse(any(LeadComment.class));
    }

    @Test
    @DisplayName("Автор не найден - бросается ResourceNotFoundException")
    void addComment_whenAuthorNotFound_shouldThrowResourceNotFoundException() {
        Long leadId = 1L;
        Long authorId = 999L;

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(existingLead));
        when(userRepository.findById(authorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.addComment(leadId, commentRequest, authorId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining(String.valueOf(authorId));

        verify(leadRepository, times(1)).findById(leadId);
        verify(userRepository, times(1)).findById(authorId);
        verify(leadCommentRepository, never()).save(any(LeadComment.class));
        verify(leadCommentMapper, never()).toResponse(any(LeadComment.class));
    }

    @Test
    @DisplayName("Проверка, что LeadCommentRepository.save() вызывается ровно один раз при успешном сценарии")
    void addComment_whenSuccessful_shouldCallSaveExactlyOnce() {
        Long leadId = 1L;
        Long authorId = 10L;

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(existingLead));
        when(userRepository.findById(authorId)).thenReturn(Optional.of(author));
        when(leadCommentRepository.save(any(LeadComment.class))).thenReturn(savedComment);
        when(leadCommentMapper.toResponse(savedComment)).thenReturn(commentResponse);

        leadService.addComment(leadId, commentRequest, authorId);

        verify(leadCommentRepository, times(1)).save(any(LeadComment.class));
    }

    @Test
    @DisplayName("Проверка, что LeadCommentRepository.save() не вызывается, если лид не найден")
    void addComment_whenLeadNotFound_shouldNotCallSave() {
        Long leadId = 999L;
        Long authorId = 10L;

        when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.addComment(leadId, commentRequest, authorId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(leadCommentRepository, never()).save(any(LeadComment.class));
        verify(leadCommentMapper, never()).toResponse(any(LeadComment.class));
    }
}
