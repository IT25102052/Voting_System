package com.slit.realityvote.validation;

import com.slit.realityvote.dto.validation.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("UserRegistrationRequest - valid credentials should pass")
    void registration_ValidCredentials_ShouldPass() {
        UserRegistrationRequest req = UserRegistrationRequest.builder()
                .username("john_doe_99")
                .email("john@realityvote.lk")
                .password("Password123@")
                .role(UserRole.VIEWER)
                .build();

        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(req);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UserRegistrationRequest - invalid password pattern should fail")
    void registration_WeakPassword_ShouldFail() {
        UserRegistrationRequest req = UserRegistrationRequest.builder()
                .username("john_doe")
                .email("john@realityvote.lk")
                .password("weakpassword")
                .role(UserRole.VIEWER)
                .build();

        Set<ConstraintViolation<UserRegistrationRequest>> violations = validator.validate(req);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("ContestantManagementRequest - age boundary check")
    void contestant_AgeBoundary_ShouldValidate() {
        ContestantManagementRequest underAge = ContestantManagementRequest.builder()
                .name("Alex Smith")
                .age(14) // below 16
                .showId(UUID.randomUUID())
                .seasonId(UUID.randomUUID())
                .imageUrl("https://cdn.realityvote.lk/alex.png")
                .status(ContestantLifecycleStatus.ACTIVE)
                .build();

        Set<ConstraintViolation<ContestantManagementRequest>> violations = validator.validate(underAge);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("age"));
    }

    @Test
    @DisplayName("JudgeScoreRequest - score 1 to 10 validation")
    void judgeScore_Range_ShouldValidate() {
        JudgeScoreRequest outOfBounds = JudgeScoreRequest.builder()
                .episodeId(UUID.randomUUID())
                .contestantId(UUID.randomUUID())
                .score(12) // above 10
                .comments("Outstanding performance")
                .build();

        Set<ConstraintViolation<JudgeScoreRequest>> violations = validator.validate(outOfBounds);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("score"));
    }

    @Test
    @DisplayName("ShowDTO - title length and URL pattern validation")
    void showDTO_Validation_ShouldValidate() {
        com.slit.realityvote.dto.ShowDTO invalid = com.slit.realityvote.dto.ShowDTO.builder()
                .title("A") // min 2
                .genre("Music")
                .bannerImageUrl("invalid-url")
                .status("UNKNOWN")
                .build();

        Set<ConstraintViolation<com.slit.realityvote.dto.ShowDTO>> violations = validator.validate(invalid);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("title"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("bannerImageUrl"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("status"));
    }

    @Test
    @DisplayName("SeasonDTO - ValidDateRange on SeasonDTO")
    void seasonDTO_DateRange_ShouldValidate() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        com.slit.realityvote.dto.SeasonDTO invalidDates = com.slit.realityvote.dto.SeasonDTO.builder()
                .showId(101L)
                .seasonNumber(1)
                .title("Season 1")
                .startDate(now)
                .endDate(now.minusDays(5)) // before start
                .status("ACTIVE")
                .build();

        Set<ConstraintViolation<com.slit.realityvote.dto.SeasonDTO>> violations = validator.validate(invalidDates);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("endDate"));
    }

    @Test
    @DisplayName("SupportTicketDTO - resolution notes length check")
    void supportTicketDTO_NotesLength_ShouldValidate() {
        com.slit.realityvote.dto.SupportTicketDTO shortNotes = com.slit.realityvote.dto.SupportTicketDTO.builder()
                .ticketId(501L)
                .resolutionNotes("Too short") // < 10 chars
                .status("RESOLVED")
                .build();

        Set<ConstraintViolation<com.slit.realityvote.dto.SupportTicketDTO>> violations = validator.validate(shortNotes);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("resolutionNotes"));
    }
}
