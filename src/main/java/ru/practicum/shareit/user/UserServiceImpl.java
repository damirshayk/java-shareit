package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(UserDto.EMAIL_REGEX);

    private final UserRepository userRepository;

    @Override
    public synchronized UserDto create(UserDto userDto) {
        checkEmailIsUnique(userDto.getEmail(), null);
        User saved = userRepository.save(UserMapper.toUser(userDto));
        return UserMapper.toUserDto(saved);
    }

    @Override
    public synchronized UserDto update(Long userId, UserDto userDto) {
        User current = findUser(userId);

        if (userDto.getName() != null) {
            validateName(userDto.getName());
            current.setName(userDto.getName());
        }
        if (userDto.getEmail() != null) {
            validateEmail(userDto.getEmail());
            checkEmailIsUnique(userDto.getEmail(), userId);
            current.setEmail(userDto.getEmail());
        }

        return UserMapper.toUserDto(userRepository.save(current));
    }

    @Override
    public UserDto getById(Long userId) {
        return UserMapper.toUserDto(findUser(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll().stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    public synchronized void delete(Long userId) {
        if (!userRepository.deleteById(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден");
        }
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(
                        "Пользователь с id " + userId + " не найден"
                ));
    }

    private void checkEmailIsUnique(String email, Long currentUserId) {
        userRepository.findByEmail(email)
                .filter(user -> !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new ConflictException("Email уже используется");
                });
    }

    private void validateName(String name) {
        if (name.isBlank()) {
            throw new ValidationException("Имя пользователя не может быть пустым");
        }
    }

    private void validateEmail(String email) {
        if (email.isBlank() || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("Email должен быть корректным");
        }
    }
}
