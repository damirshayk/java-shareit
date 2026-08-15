package ru.practicum.shareit.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createShouldRequireNameAndEmail() {
        UserDto userDto = new UserDto(null, " ", null);

        Set<String> invalidFields = validator.validate(userDto, UserDto.Create.class).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields).containsExactlyInAnyOrder("name", "email");
    }

    @Test
    void updateShouldAllowMissingFields() {
        UserDto userDto = new UserDto(null, null, null);

        assertThat(validator.validate(userDto, UserDto.Update.class)).isEmpty();
    }

    @Test
    void updateShouldRejectProvidedBlankNameAndInvalidEmail() {
        UserDto userDto = new UserDto(null, " ", "invalid-email");

        Set<String> invalidFields = validator.validate(userDto, UserDto.Update.class).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields).containsExactlyInAnyOrder("name", "email");
    }
}
