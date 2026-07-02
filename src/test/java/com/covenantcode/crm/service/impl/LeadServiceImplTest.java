package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.lead.CourseShortResponse;
import com.covenantcode.crm.dto.lead.LeadCommentResponse;
import com.covenantcode.crm.dto.lead.LeadCommentCreateRequest;
import com.covenantcode.crm.dto.lead.LeadResponse;
import com.covenantcode.crm.dto.lead.LeadCreateRequest;
import com.covenantcode.crm.dto.lead.LeadUpdateRequest;
import com.covenantcode.crm.dto.lead.LeadConvertRequest;
import com.covenantcode.crm.dto.lead.UserShortResponse;
import com.covenantcode.crm.dto.lead.LeadStatusUpdateRequest;
import com.covenantcode.crm.dto.student.StudentResponse;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Lead;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.Student;
import com.covenantcode.crm.entity.LeadComment;
import com.covenantcode.crm.entity.enums.LeadStatus;
import com.covenantcode.crm.exception.BadRequestException;
import com.covenantcode.crm.exception.ConflictException;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.LeadCommentMapper;
import com.covenantcode.crm.mapper.LeadMapper;
import com.covenantcode.crm.mapper.StudentMapper;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.StudentRepository;
import com.covenantcode.crm.repository.LeadCommentRepository;
import com.covenantcode.crm.repository.LeadRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.anyLong;  // Корректно для всех современных версий Mockito (3.x+)



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

    @Test
    @DisplayName("Возвращает все лиды постранично без фильтров")
    void getAllWithoutFiltersShouldReturnAllLeadsPaginated() {

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

        when(leadRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(leadPage);
        when(leadMapper.toResponse(lead1)).thenReturn(leadResponse1);
        when(leadMapper.toResponse(lead2)).thenReturn(leadResponse2);

        Page<LeadResponse> result = leadService.getAll(
                null,
                null,
                null,
                null,
                pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getTotalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("Фильтрует лиды по статусу NEW, спецификация передаётся в репозиторий")
    void getAllWithStatusFilterShouldApplySpecification() {

        LeadStatus status = LeadStatus.NEW;
        Pageable pageable = PageRequest.of(0, 20);

        Lead leadWithStatusNew = new Lead();
        leadWithStatusNew.setId(1L);
        leadWithStatusNew.setStatus(status);

        List<Lead> leads = List.of(leadWithStatusNew);
        Page<Lead> leadPage = new PageImpl<>(leads, pageable, leads.size());

        LeadResponse leadResponse = new LeadResponse();
        leadResponse.setId(1L);

        when(leadRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(leadPage);
        when(leadMapper.toResponse(leadWithStatusNew)).thenReturn(leadResponse);

        Page<LeadResponse> result = leadService.getAll(
                null,
                status,
                null,
                null,
                pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        ArgumentCaptor<Specification<Lead>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(leadRepository, times(1)).findAll(specCaptor.capture(), eq(pageable));

        Specification<Lead> capturedSpec = specCaptor.getValue();
        assertThat(capturedSpec).isNotNull();
    }

    @Test
    @DisplayName("Тест 1: успешное обновление данных лида")
    void update_Success_ShouldUpdateLead() {

        LeadUpdateRequest updateRequest = new LeadUpdateRequest();
        updateRequest.setFirstName("Алексей");
        updateRequest.setLastName("Смирнов");
        updateRequest.setPhone("+79162222222");
        updateRequest.setEmail("alexey@mail.ru");
        updateRequest.setSource("Реклама");
        updateRequest.setComment("Новый комментарий");
        updateRequest.setInterestedCourseId(2L);
        updateRequest.setAssignedManagerId(3L);

        Course updateCourse = new Course();
        updateCourse.setId(2L);
        updateCourse.setTitle("Курс английского");

        User updateManager = new User();
        updateManager.setId(3L);
        updateManager.setFirstName("Анна");
        updateManager.setLastName("Кузнецова");

        Lead existingLead = new Lead();
        existingLead.setId(1L);
        existingLead.setFirstName("Иван");
        existingLead.setLastName("Петров");
        existingLead.setPhone("+79161111111");
        existingLead.setEmail("ivan@mail.ru");
        existingLead.setSource("Сайт");
        existingLead.setStatus(LeadStatus.IN_PROGRESS);
        existingLead.setComment("Старый комментарий");

        Lead updatedLead = new Lead();
        updatedLead.setId(1L);
        updatedLead.setFirstName(updateRequest.getFirstName());
        updatedLead.setLastName(updateRequest.getLastName());
        updatedLead.setPhone(updateRequest.getPhone());
        updatedLead.setEmail(updateRequest.getEmail());
        updatedLead.setSource(updateRequest.getSource());
        updatedLead.setComment(updateRequest.getComment());
        updatedLead.setInterestedCourse(updateCourse);
        updatedLead.setAssignedManager(updateManager);
        updatedLead.setStatus(LeadStatus.IN_PROGRESS);
        updatedLead.setCreatedAt(OffsetDateTime.now());
        updatedLead.setUpdatedAt(OffsetDateTime.now());

        LeadResponse updatedResponse = new LeadResponse();
        updatedResponse.setId(1L);
        updatedResponse.setFirstName(updateRequest.getFirstName());
        updatedResponse.setLastName(updateRequest.getLastName());
        updatedResponse.setPhone(updateRequest.getPhone());
        updatedResponse.setEmail(updateRequest.getEmail());
        updatedResponse.setSource(updateRequest.getSource());
        updatedResponse.setComment(updateRequest.getComment());
        updatedResponse.setStatus("IN_PROGRESS");
        updatedResponse.setCreatedAt(OffsetDateTime.now().toLocalDateTime());
        updatedResponse.setUpdatedAt(OffsetDateTime.now().toLocalDateTime());

        CourseShortResponse courseShort = new CourseShortResponse();
        courseShort.setId(2L);
        courseShort.setTitle("Курс английского");
        updatedResponse.setInterestedCourse(courseShort);

        UserShortResponse userShort = new UserShortResponse();
        userShort.setId(3L);
        userShort.setFirstName("Анна");
        userShort.setLastName("Кузнецова");
        updatedResponse.setAssignedManager(userShort);

        when(leadRepository.findById(1L)).thenReturn(Optional.of(existingLead));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(updateCourse));
        when(userRepository.findById(3L)).thenReturn(Optional.of(updateManager));
        when(leadRepository.save(any(Lead.class))).thenReturn(updatedLead);
        when(leadMapper.toResponse(any(Lead.class))).thenReturn(updatedResponse);

        LeadResponse response = leadService.update(1L, updateRequest);

        assertThat(response).isNotNull();

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFirstName()).isEqualTo("Алексей");
        assertThat(response.getLastName()).isEqualTo("Смирнов");
        assertThat(response.getPhone()).isEqualTo("+79162222222");
        assertThat(response.getEmail()).isEqualTo("alexey@mail.ru");
        assertThat(response.getSource()).isEqualTo("Реклама");
        assertThat(response.getComment()).isEqualTo("Новый комментарий");
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.getInterestedCourse()).isNotNull();
        assertThat(response.getInterestedCourse().getId()).isEqualTo(2L);

        verify(leadRepository).findById(1L);
        verify(courseRepository).findById(2L);
        verify(userRepository).findById(3L);
        verify(leadRepository).save(any(Lead.class));
        verify(leadMapper).toResponse(any(Lead.class));

        ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(leadCaptor.capture());
        Lead capturedLead = leadCaptor.getValue();
        assertThat(capturedLead).isNotNull();
        assertThat(capturedLead.getStatus()).isEqualTo(LeadStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Тест 2: попытка обновить конвертированного лида → BadRequestException")
    void update_ConvertedLead_ShouldThrowBadRequestException() {

        LeadUpdateRequest updateRequest = new LeadUpdateRequest();
        updateRequest.setFirstName("Алексей");
        updateRequest.setLastName("Смирнов");
        updateRequest.setPhone("+79162222222");
        updateRequest.setEmail("alexey@mail.ru");
        updateRequest.setSource("Реклама");
        updateRequest.setComment("Новый комментарий");
        updateRequest.setInterestedCourseId(2L);
        updateRequest.setAssignedManagerId(3L);

        Lead existingLead = new Lead();
        existingLead.setId(1L);
        existingLead.setFirstName("Иван");
        existingLead.setLastName("Петров");
        existingLead.setPhone("+79161111111");
        existingLead.setEmail("ivan@mail.ru");
        existingLead.setSource("Сайт");
        existingLead.setStatus(LeadStatus.CONVERTED_TO_STUDENT);
        existingLead.setComment("Конвертирован");

        when(leadRepository.findById(1L)).thenReturn(Optional.of(existingLead));

        assertThatThrownBy(() -> leadService.update(1L, updateRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Нельзя редактировать конвертированного лида");

        verify(leadRepository).findById(1L);
        verify(courseRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
        verify(leadRepository, never()).save(any(Lead.class));
        verify(leadMapper, never()).toResponse(any(Lead.class));
    }

    @Test
    @DisplayName("Тест 3: лид не найден → ResourceNotFoundException")
    void update_LeadNotFound_ShouldThrowResourceNotFoundException() {

        LeadUpdateRequest updateRequest = new LeadUpdateRequest();
        updateRequest.setFirstName("Алексей");
        updateRequest.setPhone("+79162222222");

        when(leadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.update(99L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Lead с id 99 не найден");

        verify(leadRepository).findById(99L);
        verify(courseRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
        verify(leadRepository, never()).save(any(Lead.class));
        verify(leadMapper, never()).toResponse(any(Lead.class));
    }

    @Test
    @DisplayName("Тест 4: курс не найден → ResourceNotFoundException")
    void update_CourseNotFound_ShouldThrowResourceNotFoundException() {

        LeadUpdateRequest updateRequest = new LeadUpdateRequest();
        updateRequest.setFirstName("Алексей");
        updateRequest.setLastName("Смирнов");
        updateRequest.setPhone("+79162222222");
        updateRequest.setEmail("alexey@mail.ru");
        updateRequest.setSource("Реклама");
        updateRequest.setComment("Новый комментарий");
        updateRequest.setInterestedCourseId(2L);
        updateRequest.setAssignedManagerId(3L);

        Lead existingLead = new Lead();
        existingLead.setId(1L);
        existingLead.setFirstName("Иван");
        existingLead.setLastName("Петров");
        existingLead.setPhone("+79161111111");
        existingLead.setEmail("ivan@mail.ru");
        existingLead.setSource("Сайт");
        existingLead.setStatus(LeadStatus.IN_PROGRESS);
        existingLead.setComment("Старый комментарий");

        when(leadRepository.findById(1L)).thenReturn(Optional.of(existingLead));
        when(courseRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.update(1L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Course с id 2 не найден");

        verify(leadRepository).findById(1L);
        verify(courseRepository).findById(2L);
        verify(userRepository, never()).findById(any());
        verify(leadRepository, never()).save(any(Lead.class));
        verify(leadMapper, never()).toResponse(any(Lead.class));
    }

    @Test
    @DisplayName("Тест 5: менеджер не найден → ResourceNotFoundException")
    void update_ManagerNotFound_ShouldThrowResourceNotFoundException() {

        LeadUpdateRequest updateRequest = new LeadUpdateRequest();
        updateRequest.setFirstName("Алексей");
        updateRequest.setLastName("Смирнов");
        updateRequest.setPhone("+79162222222");
        updateRequest.setEmail("alexey@mail.ru");
        updateRequest.setSource("Реклама");
        updateRequest.setComment("Новый комментарий");
        updateRequest.setInterestedCourseId(2L);
        updateRequest.setAssignedManagerId(3L);

        Course updateCourse = new Course();
        updateCourse.setId(2L);
        updateCourse.setTitle("Курс английского");

        Lead existingLead = new Lead();
        existingLead.setId(1L);
        existingLead.setFirstName("Иван");
        existingLead.setLastName("Петров");
        existingLead.setPhone("+79161111111");
        existingLead.setEmail("ivan@mail.ru");
        existingLead.setSource("Сайт");
        existingLead.setStatus(LeadStatus.IN_PROGRESS);
        existingLead.setComment("Старый комментарий");

        when(leadRepository.findById(1L)).thenReturn(Optional.of(existingLead));
        when(courseRepository.findById(2L)).thenReturn(Optional.of(updateCourse));
        when(userRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.update(1L, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User с id 3 не найден");

        verify(leadRepository).findById(1L);
        verify(courseRepository).findById(2L);
        verify(userRepository).findById(3L);
        verify(leadRepository, never()).save(any(Lead.class));
        verify(leadMapper, never()).toResponse(any(Lead.class));
    }

    @Test
    @DisplayName("Тест 6: сброс курса и менеджера при null")
    void update_NullCourseAndManager_ShouldClearRelations() {

        LeadUpdateRequest updateRequest = new LeadUpdateRequest();
        updateRequest.setFirstName("Алексей");
        updateRequest.setLastName("Смирнов");
        updateRequest.setPhone("+79162222222");
        updateRequest.setEmail("alexey@mail.ru");
        updateRequest.setSource("Реклама");
        updateRequest.setComment("Новый комментарий");
        updateRequest.setInterestedCourseId(null);
        updateRequest.setAssignedManagerId(null);

        Lead existingLead = new Lead();
        existingLead.setId(1L);
        existingLead.setFirstName("Иван");
        existingLead.setLastName("Петров");
        existingLead.setPhone("+79161111111");
        existingLead.setEmail("ivan@mail.ru");
        existingLead.setSource("Сайт");
        existingLead.setStatus(LeadStatus.IN_PROGRESS);
        existingLead.setComment("Старый комментарий");

        Lead clearedLead = new Lead();
        clearedLead.setId(1L);
        clearedLead.setFirstName(updateRequest.getFirstName());
        clearedLead.setLastName(updateRequest.getLastName());
        clearedLead.setPhone(updateRequest.getPhone());
        clearedLead.setEmail(updateRequest.getEmail());
        clearedLead.setSource(updateRequest.getSource());
        clearedLead.setComment(updateRequest.getComment());
        clearedLead.setInterestedCourse(null);
        clearedLead.setAssignedManager(null);
        clearedLead.setStatus(LeadStatus.IN_PROGRESS);

        LeadResponse clearedResponse = new LeadResponse();
        clearedResponse.setId(1L);
        clearedResponse.setFirstName(updateRequest.getFirstName());
        clearedResponse.setPhone(updateRequest.getPhone());
        clearedResponse.setStatus("IN_PROGRESS");
        clearedResponse.setInterestedCourse(null);
        clearedResponse.setAssignedManager(null);

        when(leadRepository.findById(1L)).thenReturn(Optional.of(existingLead));
        when(leadRepository.save(any(Lead.class))).thenReturn(clearedLead);
        when(leadMapper.toResponse(any(Lead.class))).thenReturn(clearedResponse);

        LeadResponse response = leadService.update(1L, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.getInterestedCourse()).isNull();
        assertThat(response.getAssignedManager()).isNull();

        ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository).save(leadCaptor.capture());
        Lead capturedLead = leadCaptor.getValue();
        assertThat(capturedLead.getInterestedCourse()).isNull();
        assertThat(capturedLead.getAssignedManager()).isNull();

        verify(courseRepository, never()).findById(any());
        verify(userRepository, never()).findById(any());
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

    @Test
    @DisplayName("Успешное обновление статуса – возвращает LeadResponse и вызывает save ровно один раз")
    void updateStatus_success_shouldReturnLeadResponse() {
        Long leadId = 1L;
        LeadStatus newStatus = LeadStatus.IN_PROGRESS;
        LeadStatusUpdateRequest request = new LeadStatusUpdateRequest();
        request.setStatus(newStatus);

        Lead existingLead = new Lead();
        existingLead.setId(leadId);
        existingLead.setStatus(LeadStatus.NEW);

        Lead updatedLead = new Lead();
        updatedLead.setId(leadId);
        updatedLead.setStatus(newStatus);

        LeadResponse expectedResponse = new LeadResponse();
        expectedResponse.setId(leadId);
        expectedResponse.setStatus(newStatus.name());

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(existingLead));
        when(leadRepository.save(any(Lead.class))).thenReturn(updatedLead);
        when(leadMapper.toResponse(updatedLead)).thenReturn(expectedResponse);

        LeadResponse result = leadService.updateStatus(leadId, request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(leadId);
        assertThat(result.getStatus()).isEqualTo(newStatus.name());

        verify(leadRepository, times(1)).findById(leadId);
        verify(leadRepository, times(1)).save(any(Lead.class));
        verify(leadMapper, times(1)).toResponse(updatedLead);
        verifyNoMoreInteractions(leadRepository, leadMapper);
    }

    @Test
    @DisplayName("Лид не найден – бросается ResourceNotFoundException и save не вызывается")
    void updateStatus_leadNotFound_shouldThrowResourceNotFoundException() {
        Long leadId = 99L;
        LeadStatusUpdateRequest request = new LeadStatusUpdateRequest();
        request.setStatus(LeadStatus.NEW);

        when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.updateStatus(leadId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Lead с id " + leadId + " не найден");

        verify(leadRepository, times(1)).findById(leadId);
        verify(leadRepository, never()).save(any(Lead.class));
        verifyNoInteractions(leadMapper);
    }

    @Test
    @DisplayName("Передан статус CONVERTED_TO_STUDENT – бросается ConflictException и save не вызывается")
    void updateStatus_statusConvertedToStudent_shouldThrowConflictException() {
        Long leadId = 1L;
        LeadStatusUpdateRequest request = new LeadStatusUpdateRequest();
        request.setStatus(LeadStatus.CONVERTED_TO_STUDENT);

        Lead existingLead = new Lead();
        existingLead.setId(leadId);
        existingLead.setStatus(LeadStatus.NEW);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(existingLead));

        assertThatThrownBy(() -> leadService.updateStatus(leadId, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Статус CONVERTED_TO_STUDENT нельзя установить вручную. Используйте POST /api/v1/leads/" + leadId + "/convert");

        verify(leadRepository, times(1)).findById(leadId);
        verify(leadRepository, never()).save(any(Lead.class));
        verifyNoInteractions(leadMapper);
    }

    @Test
    @DisplayName("Возвращает список комментариев для существующего лида, отсортированных по createdAt ASC")
    void getComments_shouldReturnSortedComments_whenLeadExists() {
        // Arrange
        Long leadId = 1L;

        // Создаем два комментария с разными датами (старый и новый)
        LeadComment oldComment = LeadComment.builder()
                .id(200L)
                .lead(existingLead)
                .author(author)
                .text("Old comment")
                .createdAt(OffsetDateTime.now().minusDays(1)) // Старый (первый в сортировке)
                .build();

        LeadComment newComment = LeadComment.builder()
                .id(100L)  // ID из savedComment
                .lead(existingLead)
                .author(author)
                .text("Test comment text")  // Текст из savedComment
                .createdAt(OffsetDateTime.now())  // Новый (второй в сортировке)
                .build();

        // Настраиваем mock для отображения комментариев в обратном порядке (сперва старый)
        when(leadRepository.existsById(leadId)).thenReturn(true);
        when(leadCommentRepository.findByLeadIdOrderByCreatedAtAsc(leadId))
                .thenReturn(List.of(oldComment, newComment));

        // Mock для отображения
        LeadCommentResponse oldResponse = LeadCommentResponse.builder()
                .id(200L)
                .leadId(leadId)
                .author(new UserShortResponse(author.getId(), author.getFirstName(), author.getLastName()))
                .text("Old comment")
                .createdAt(oldComment.getCreatedAt().toLocalDateTime())
                .build();

        LeadCommentResponse newResponse = LeadCommentResponse.builder()
                .id(100L)
                .leadId(leadId)
                .author(new UserShortResponse(author.getId(), author.getFirstName(), author.getLastName()))
                .text("Test comment text")
                .createdAt(newComment.getCreatedAt().toLocalDateTime())
                .build();

        when(leadCommentMapper.toResponse(oldComment)).thenReturn(oldResponse);
        when(leadCommentMapper.toResponse(newComment)).thenReturn(newResponse);

        // Act
        List<LeadCommentResponse> result = leadService.getComments(leadId);

        // Assert
        assertThat(result).isNotNull().hasSize(2);

        // Проверяем, что старый комментарий первый (сортировка по возрастанию createdAt)
        assertThat(result.get(0).getId()).isEqualTo(200L);  // Старый (первый)
        assertThat(result.get(1).getId()).isEqualTo(100L);  // Новый (второй)

        // Проверка вызовов
        verify(leadRepository, times(1)).existsById(leadId);
        verify(leadCommentRepository, times(1)).findByLeadIdOrderByCreatedAtAsc(leadId);
        verify(leadCommentMapper, times(1)).toResponse(oldComment);
        verify(leadCommentMapper, times(1)).toResponse(newComment);
    }

    @Test
    @DisplayName("Возвращает пустой список комментариев, если у лида их нет")
    void getComments_shouldReturnEmptyList_whenNoComments() {
        // Arrange
        Long leadId = 1L;
        when(leadRepository.existsById(leadId)).thenReturn(true);
        when(leadCommentRepository.findByLeadIdOrderByCreatedAtAsc(leadId))
                .thenReturn(Collections.emptyList());

        // Act
        List<LeadCommentResponse> result = leadService.getComments(leadId);

        // Assert
        assertThat(result).isEmpty();
        verify(leadCommentRepository, times(1)).findByLeadIdOrderByCreatedAtAsc(leadId);
        verify(leadCommentMapper, never()).toResponse(any(LeadComment.class)); // Маппер не вызывается
    }

    @Test
    @DisplayName("Бросает ResourceNotFoundException, если лид не найден")
    void getComments_shouldThrowException_whenLeadNotFound() {
        // Arrange
        Long leadId = 999L;
        when(leadRepository.existsById(leadId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> leadService.getComments(leadId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Lead с id " + leadId + " не найден");

        // Проверяем, что репозиторий не вызывался
        verify(leadRepository, times(1)).existsById(leadId);
        verify(leadCommentRepository, never()).findByLeadIdOrderByCreatedAtAsc(leadId);
    }


   @Test
    @DisplayName("Проверяет, что findByLeadIdOrderByCreatedAtAsc вызывается ровно один раз при успешном сценарии")
    void getComments_shouldCallFindByLeadIdOnce_whenLeadExists() {
        // Arrange
        Long leadId = 1L;
        when(leadRepository.existsById(leadId)).thenReturn(true);
        when(leadCommentRepository.findByLeadIdOrderByCreatedAtAsc(leadId))
                .thenReturn(List.of(savedComment));
        when(leadCommentMapper.toResponse(savedComment)).thenReturn(commentResponse);

        // Act
        leadService.getComments(leadId);

        // Assert
        verify(leadCommentRepository, times(1)).findByLeadIdOrderByCreatedAtAsc(leadId);
    }
    @Test
    @DisplayName("Не вызывается findByLeadIdOrderByCreatedAtAsc, если лид не найден")
    void getComments_shouldNotCallFindByLeadId_whenLeadNotFound() {
        // Arrange
        Long leadId = 999L;
        when(leadRepository.existsById(leadId)).thenReturn(false);

        // Act
        assertThatThrownBy(() -> leadService.getComments(leadId))
                .isInstanceOf(ResourceNotFoundException.class);

        // Assert
        verify(leadRepository, times(1)).existsById(leadId);
        verify(leadCommentRepository, never()).findByLeadIdOrderByCreatedAtAsc(leadId);
    }
}

