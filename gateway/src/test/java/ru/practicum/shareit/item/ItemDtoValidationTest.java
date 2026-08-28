package ru.practicum.shareit.item;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ItemDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void createShouldRequireAllItemFields() {
        ItemDto itemDto = new ItemDto(null, " ", null, null, null);

        Set<ConstraintViolation<ItemDto>> violations = validator.validate(itemDto, ItemDto.Create.class);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("name", "description", "available");
    }

    @Test
    void updateShouldAllowMissingFields() {
        ItemDto itemDto = new ItemDto(null, null, null, null, null);

        Set<ConstraintViolation<ItemDto>> violations = validator.validate(itemDto, ItemDto.Update.class);

        assertThat(violations).isEmpty();
    }

    @Test
    void updateShouldRejectProvidedBlankText() {
        ItemDto itemDto = new ItemDto(null, " ", "\t", null, null);

        Set<ConstraintViolation<ItemDto>> violations = validator.validate(itemDto, ItemDto.Update.class);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("name", "description");
    }

    @Test
    void commentShouldRequireText() {
        CommentDto commentDto = new CommentDto(" ");

        Set<ConstraintViolation<CommentDto>> violations = validator.validate(commentDto);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("text");
    }
}
