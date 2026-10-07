package com.slit.realityvote.validation;

import com.slit.realityvote.dto.validation.SeasonManagementRequest;
import com.slit.realityvote.dto.validation.SeasonStatus;
import com.slit.realityvote.dto.validation.SessionLifecycleStatus;
import com.slit.realityvote.dto.validation.VotingSessionManagementRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DateRangeValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid date range should pass validation")
    void validDateRange_ShouldPass() {
        LocalDateTime now = LocalDateTime.now();
        SeasonManagementRequest request = SeasonManagementRequest.builder()
                .showId(UUID.randomUUID())
                .seasonNumber(1)
                .title("Season 1")
                .startDate(now)
                .endDate(now.plusMonths(3))
                .status(SeasonStatus.ACTIVE)
                .build();

        Set<ConstraintViolation<SeasonManagementRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("End date before start date should fail validation on endDate property")
    void endDateBeforeStartDate_ShouldFail() {
        LocalDateTime now = LocalDateTime.now();
        SeasonManagementRequest request = SeasonManagementRequest.builder()
                .showId(UUID.randomUUID())
                .seasonNumber(1)
                .title("Season 1")
                .startDate(now)
                .endDate(now.minusDays(1))
                .status(SeasonStatus.ACTIVE)
                .build();

        Set<ConstraintViolation<SeasonManagementRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        ConstraintViolation<SeasonManagementRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("endDate");
        assertThat(violation.getMessage()).isEqualTo("End date must be strictly after start date");
    }

    @Test
    @DisplayName("End time equal to start time should fail when allowEqual is false")
    void endTimeEqualToStartTime_ShouldFail() {
        LocalDateTime now = LocalDateTime.now();
        VotingSessionManagementRequest request = VotingSessionManagementRequest.builder()
                .episodeId(UUID.randomUUID())
                .sessionName("Live Finale Voting")
                .startTime(now)
                .endTime(now)
                .maxVotesPerUser(5)
                .status(SessionLifecycleStatus.ACTIVE)
                .build();

        Set<ConstraintViolation<VotingSessionManagementRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        ConstraintViolation<VotingSessionManagementRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("endTime");
        assertThat(violation.getMessage()).isEqualTo("End time must be strictly after start time");
    }
}
