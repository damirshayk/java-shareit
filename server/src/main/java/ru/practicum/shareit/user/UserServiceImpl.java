package ru.practicum.shareit.user;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(UserDto.EMAIL_REGEX);

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto create(UserDto userDto) {
        checkEmailIsUnique(userDto.getEmail(), null);
        try {
            return UserMapper.toUserDto(userRepository.saveAndFlush(UserMapper.toUser(userDto)));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Email уже используется");
        }
    }

    @Override
    @Transactional
    public UserDto update(Long userId, UserDto userDto) {
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

        try {
            return UserMapper.toUserDto(userRepository.saveAndFlush(current));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Email уже используется");
        }
    }

    @Override
    public UserDto getById(Long userId) {
        return UserMapper.toUserDto(findUser(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        if (userRepository.deleteUserById(userId) == 0) {
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
        userRepository.findByEmailIgnoreCase(email)
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
