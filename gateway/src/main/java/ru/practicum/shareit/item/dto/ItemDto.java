package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {

    private Long id;

    @NotBlank(message = "Название вещи не может быть пустым", groups = Create.class)
    @Pattern(
            regexp = ".*\\S.*",
            message = "Название вещи не может быть пустым",
            groups = Update.class
    )
    private String name;

    @NotBlank(message = "Описание вещи не может быть пустым", groups = Create.class)
    @Pattern(
            regexp = ".*\\S.*",
            message = "Описание вещи не может быть пустым",
            groups = Update.class
    )
    private String description;

    @NotNull(message = "Статус доступности должен быть указан", groups = Create.class)
    private Boolean available;

    private Long requestId;

    public interface Create {
    }

    public interface Update {
    }
}
