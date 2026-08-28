package ru.practicum.shareit.booking;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;

import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BookingDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void bookingShouldRequireItemAndDates() {
        BookItemRequestDto booking = new BookItemRequestDto(null, null, null);

        Set<ConstraintViolation<BookItemRequestDto>> violations = validator.validate(booking);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("itemId", "start", "end");
    }

    @Test
    void bookingShouldRejectEndBeforeStart() {
        BookItemRequestDto booking = new BookItemRequestDto(
                1L,
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(1)
        );

        assertThat(validator.validate(booking))
                .extracting(ConstraintViolation::getMessage)
                .contains("Дата окончания должна быть позже даты начала");
    }

    @Test
    void bookingShouldAcceptValidDates() {
        BookItemRequestDto booking = new BookItemRequestDto(
                1L,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2)
        );

        assertThat(validator.validate(booking)).isEmpty();
    }
}
