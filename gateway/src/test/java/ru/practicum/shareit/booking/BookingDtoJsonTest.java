package ru.practicum.shareit.booking;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.booking.dto.BookItemRequestDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class BookingDtoJsonTest {

    @Autowired
    private JacksonTester<BookItemRequestDto> json;

    @Test
    void shouldSerializeBookingRequest() throws Exception {
        BookItemRequestDto request = new BookItemRequestDto(
                42L,
                LocalDateTime.of(2030, 1, 2, 10, 30),
                LocalDateTime.of(2030, 1, 3, 12, 45)
        );

        assertThat(json.write(request))
                .extractingJsonPathNumberValue("@.itemId")
                .isEqualTo(42);
        assertThat(json.write(request))
                .extractingJsonPathStringValue("@.start")
                .isEqualTo("2030-01-02T10:30:00");
        assertThat(json.write(request))
                .extractingJsonPathStringValue("@.end")
                .isEqualTo("2030-01-03T12:45:00");
        assertThat(json.write(request)).doesNotHaveJsonPath("@.dateRangeValid");
    }

    @Test
    void shouldDeserializeBookingRequest() throws Exception {
        String content = String.join(
                ",",
                "{\"itemId\":42",
                "\"start\":\"2030-01-02T10:30:00\"",
                "\"end\":\"2030-01-03T12:45:00\"}"
        );
        BookItemRequestDto request = json.parseObject(content);

        assertThat(request.getItemId()).isEqualTo(42L);
        assertThat(request.getStart()).isEqualTo(LocalDateTime.of(2030, 1, 2, 10, 30));
        assertThat(request.getEnd()).isEqualTo(LocalDateTime.of(2030, 1, 3, 12, 45));
    }
}
