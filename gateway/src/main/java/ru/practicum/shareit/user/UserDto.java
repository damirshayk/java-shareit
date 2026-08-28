package ru.practicum.shareit.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    public static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)*$";

    private Long id;

    @NotBlank(message = "Имя пользователя не может быть пустым", groups = Create.class)
    @Pattern(
            regexp = ".*\\S.*",
            message = "Имя пользователя не может быть пустым",
            groups = Update.class
    )
    private String name;

    @NotBlank(message = "Email не может быть пустым", groups = Create.class)
    @Pattern(
            regexp = EMAIL_REGEX,
            message = "Email должен быть корректным",
            groups = {Create.class, Update.class}
    )
    private String email;

    public interface Create {
    }

    public interface Update {
    }
}
