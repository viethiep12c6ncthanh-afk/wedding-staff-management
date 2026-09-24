package com.viethiep.weddingstaff.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viethiep.weddingstaff.config.AiRecommendationProperties;
import com.viethiep.weddingstaff.dto.AiRecommendationResponse;
import com.viethiep.weddingstaff.dto.AiConnectionTestResponse;
import com.viethiep.weddingstaff.dto.ReplacementCandidateResponse;
import com.viethiep.weddingstaff.entity.*;
import com.viethiep.weddingstaff.enumtype.*;
import com.viethiep.weddingstaff.repository.AiRecommendationRunRepository;
import com.viethiep.weddingstaff.repository.EmployeeEvaluationRepository;
import com.viethiep.weddingstaff.repository.ReplacementRequestRepository;
import com.viethiep.weddingstaff.repository.UserAccountRepository;
import com.viethiep.weddingstaff.service.ai.AiRecommendationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiRecommendationServiceTest {
    @Mock
    private CandidateRecommendationService candidateService;
    @Mock
    private ReplacementRequestRepository requestRepository;
    @Mock
    private EmployeeEvaluationRepository evaluationRepository;
    @Mock
    private UserAccountRepository userRepository;
    @Mock
    private AiRecommendationRunRepository runRepository;
    @Mock
    private AiRecommendationClient aiClient;

    private AiRecommendationProperties properties;
    private AiRecommendationService service;
    private ReplacementRequest request;
    private UserAccount actor;
    private ReplacementCandidateResponse first;
    private ReplacementCandidateResponse second;

    @BeforeEach
    void setUp() {
        properties = new AiRecommendationProperties();
        properties.setCandidateLimit(5);
        properties.setRecentEvaluationLimit(3);
        service = new AiRecommendationService(
                candidateService,
                requestRepository,
                evaluationRepository,
                userRepository,
                runRepository,
                aiClient,
                properties,
                new ObjectMapper()
        );

        Venue venue = Venue.builder()
                .id(1L)
                .name("Riverside")
                .address("TP.HCM")
                .build();
        Event event = Event.builder()
                .id(2L)
                .venue(venue)
                .name("Tiệc cưới")
                .startAt(LocalDateTime.now().plusDays(2))
                .endAt(LocalDateTime.now().plusDays(2).plusHours(6))
                .eventStatus(EventStatus.CONFIRMED)
                .build();
        WorkShift shift = WorkShift.builder()
                .id(3L)
                .event(event)
                .name("Ca tối")
                .startAt(LocalDateTime.now().plusDays(2).plusHours(1))
                .endAt(LocalDateTime.now().plusDays(2).plusHours(5))
                .requiredStaff(3)
                .payAmount(BigDecimal.valueOf(300000))
                .shiftStatus(ShiftStatus.OPEN)
                .build();
        Role adminRole = Role.builder().id(1L).name(RoleName.ADMIN).build();
        actor = UserAccount.builder()
                .id(10L)
                .username("admin")
                .fullName("Admin")
                .role(adminRole)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        Employee originalEmployee = employee(20L, "NV020");
        ShiftArea area = ShiftArea.builder()
                .id(5L)
                .shift(shift)
                .name("VIP")
                .areaStatus(CommonStatus.ACTIVE)
                .build();
        ShiftAssignment originalAssignment = ShiftAssignment.builder()
                .id(30L)
                .shift(shift)
                .employee(originalEmployee)
                .shiftRole(ShiftRole.STAFF)
                .shiftArea(area)
                .task("Phục vụ bàn")
                .assignmentSource(AssignmentSource.DIRECT)
                .status(AssignmentStatus.CANCELLED)
                .assignedBy(actor)
                .build();
        request = ReplacementRequest.builder()
                .id(40L)
                .originalAssignment(originalAssignment)
                .requestedBy(originalEmployee.getUser())
                .reason("Việc cá nhân")
                .status(ReplacementRequestStatus.OPEN)
                .build();

        first = candidate(1, 21L, "NV021", "Nguyễn A", 80);
        second = candidate(2, 22L, "NV022", "Nguyễn B", 75);
        lenient().when(candidateService.findCandidates(40L)).thenReturn(List.of(first, second));
        lenient().when(requestRepository.findByIdWithDetails(40L)).thenReturn(Optional.of(request));
        lenient().when(userRepository.findByUsername("admin")).thenReturn(Optional.of(actor));
        lenient().when(evaluationRepository.findAllByEmployeeId(anyLong())).thenReturn(List.of());
        when(aiClient.provider()).thenReturn("OPENAI");
        when(aiClient.model()).thenReturn("test-model");
        AtomicLong ids = new AtomicLong(100);
        lenient().when(runRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            AiRecommendationRun run = invocation.getArgument(0);
            run.setId(ids.getAndIncrement());
            return run;
        });
    }

    @Test
    void validAiResultCanRerankOnlyExistingCandidates() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(aiClient.recommend(any())).thenReturn(new AiRecommendationClient.Result(
                "{\"summary\":\"B phù hợp hơn\"}",
                "B phù hợp hơn với ngữ cảnh ca.",
                List.of(
                        new AiRecommendationClient.CandidateAnalysis(
                                22L,
                                "Có tín hiệu đánh giá mềm phù hợp hơn.",
                                List.of("Phù hợp ngữ cảnh"),
                                List.of("Điểm deterministic thấp hơn A")
                        ),
                        new AiRecommendationClient.CandidateAnalysis(
                                21L,
                                "Điểm nền cao nhưng ít tín hiệu mềm hơn.",
                                List.of("Điểm deterministic cao"),
                                List.of()
                        )
                )
        ));

        AiRecommendationResponse response = service.recommend(40L, "admin");

        assertEquals(AiRecommendationMode.AI_ASSISTED, response.mode());
        assertFalse(response.fallbackUsed());
        assertEquals(22L, response.candidates().get(0).employeeId());
        assertEquals(2, response.candidates().get(0).deterministicRank());
        assertEquals(75, response.candidates().get(0).deterministicScore());
        verify(runRepository).saveAndFlush(argThat(run ->
                run.getMode() == AiRecommendationMode.AI_ASSISTED
                        && !run.isFallbackUsed()
                        && run.getAiResultJson() != null
        ));
    }

    @Test
    void connectionTestUsesRealDatabaseCandidatesWithoutDemoIdentity() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(candidateService.findRealCandidatesForAiTest(5))
                .thenReturn(List.of(first, second));
        when(aiClient.recommend(any())).thenReturn(new AiRecommendationClient.Result(
                "{}",
                "Đã phân tích dữ liệu nhân viên thật.",
                List.of(analysis(22L), analysis(21L))
        ));

        AiConnectionTestResponse response = service.testConnection();

        assertEquals("MYSQL_REAL_EMPLOYEES", response.dataSource());
        assertEquals(2, response.inputCandidateCount());
        assertEquals("NV022", response.candidates().get(0).employeeCode());
        assertEquals("Nguyễn B", response.candidates().get(0).fullName());
        assertTrue(response.candidates().stream()
                .noneMatch(candidate -> candidate.employeeCode().startsWith("DEMO-")));
    }

    @Test
    void disabledAiFallsBackWithoutCallingProvider() {
        when(aiClient.isAvailable()).thenReturn(false);

        AiRecommendationResponse response = service.recommend(40L, "admin");

        assertEquals(AiRecommendationMode.DETERMINISTIC_FALLBACK, response.mode());
        assertTrue(response.fallbackUsed());
        assertEquals("AI_DISABLED_OR_NOT_CONFIGURED", response.fallbackReason());
        assertEquals(List.of(21L, 22L), response.candidates().stream()
                .map(candidate -> candidate.employeeId())
                .toList());
        verify(aiClient, never()).recommend(any());
    }

    @Test
    void providerFailureFallsBackToDeterministicRanking() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(aiClient.recommend(any())).thenThrow(new IllegalStateException("timeout"));

        AiRecommendationResponse response = service.recommend(40L, "admin");

        assertTrue(response.fallbackUsed());
        assertEquals("AI_PROVIDER_ERROR", response.fallbackReason());
        assertEquals(21L, response.candidates().get(0).employeeId());
    }

    @Test
    void unknownEmployeeIdFromAiIsRejected() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(aiClient.recommend(any())).thenReturn(new AiRecommendationClient.Result(
                "{}",
                "summary",
                List.of(
                        analysis(999L),
                        analysis(21L)
                )
        ));

        AiRecommendationResponse response = service.recommend(40L, "admin");

        assertTrue(response.fallbackUsed());
        assertEquals("AI_OUTPUT_INVALID", response.fallbackReason());
        assertEquals(21L, response.candidates().get(0).employeeId());
    }

    @Test
    void duplicateOrMissingCandidateFromAiIsRejected() {
        when(aiClient.isAvailable()).thenReturn(true);
        when(aiClient.recommend(any())).thenReturn(new AiRecommendationClient.Result(
                "{}",
                "summary",
                List.of(
                        analysis(21L),
                        analysis(21L)
                )
        ));

        AiRecommendationResponse response = service.recommend(40L, "admin");

        assertTrue(response.fallbackUsed());
        assertEquals("AI_OUTPUT_INVALID", response.fallbackReason());
    }

    @Test
    void noEligibleCandidatesSkipsAiAndPersistsFallback() {
        when(candidateService.findCandidates(40L)).thenReturn(List.of());

        AiRecommendationResponse response = service.recommend(40L, "admin");

        assertTrue(response.fallbackUsed());
        assertEquals("NO_ELIGIBLE_CANDIDATES", response.fallbackReason());
        assertTrue(response.candidates().isEmpty());
        verify(aiClient, never()).recommend(any());
    }

    private AiRecommendationClient.CandidateAnalysis analysis(Long employeeId) {
        return new AiRecommendationClient.CandidateAnalysis(
                employeeId,
                "Phân tích hợp lệ",
                List.of("Điểm mạnh"),
                List.of()
        );
    }

    private ReplacementCandidateResponse candidate(
            int rank,
            Long employeeId,
            String code,
            String fullName,
            int score
    ) {
        return new ReplacementCandidateResponse(
                rank,
                employeeId,
                code,
                fullName,
                score,
                80,
                40,
                80,
                20,
                0,
                0,
                0,
                0,
                List.of("Không trùng lịch")
        );
    }

    private Employee employee(Long id, String code) {
        Role role = Role.builder().id(2L).name(RoleName.EMPLOYEE).build();
        UserAccount user = UserAccount.builder()
                .id(id + 100)
                .username(code.toLowerCase())
                .fullName(code)
                .role(role)
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        return Employee.builder()
                .id(id)
                .employeeCode(code)
                .employmentStatus(EmployeeStatus.ACTIVE)
                .user(user)
                .build();
    }
}
