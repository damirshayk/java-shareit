package ru.practicum.shareit.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookItemRequestDto {

    @NotNull(message = "Идентификатор вещи должен быть указан")
    private Long itemId;

    @NotNull(message = "Дата начала бронирования должна быть указана")
    @Future(message = "Дата начала бронирования должна быть в будущем")
    private LocalDateTime start;

    @NotNull(message = "Дата окончания бронирования должна быть указана")
    @Future(message = "Дата окончания бронирования должна быть в будущем")
    private LocalDateTime end;

    @JsonIgnore
    @AssertTrue(message = "Дата окончания должна быть позже даты начала")
    public boolean isDateRangeValid() {
        return start == null || end == null || end.isAfter(start);
    }
}
