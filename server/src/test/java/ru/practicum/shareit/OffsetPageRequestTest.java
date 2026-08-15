package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OffsetPageRequestTest {

    @Test
    void shouldPreserveExactOffsetAndNavigatePages() {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        OffsetPageRequest page = new OffsetPageRequest(5, 3, sort);

        assertThat(page.getOffset()).isEqualTo(5);
        assertThat(page.getPageSize()).isEqualTo(3);
        assertThat(page.getPageNumber()).isEqualTo(1);
        assertThat(page.getSort()).isEqualTo(sort);
        assertThat(page.hasPrevious()).isTrue();
        assertThat(page.next().getOffset()).isEqualTo(8);
        assertThat(page.previousOrFirst().getOffset()).isEqualTo(2);
        assertThat(page.first().getOffset()).isZero();
        assertThat(page.withPage(4).getOffset()).isEqualTo(12);
    }

    @Test
    void firstPageShouldNotHavePreviousPage() {
        OffsetPageRequest page = new OffsetPageRequest(0, 10, Sort.unsorted());

        assertThat(page.hasPrevious()).isFalse();
        assertThat(page.previousOrFirst().getOffset()).isZero();
    }

    @Test
    void shouldRejectInvalidPagination() {
        assertThatThrownBy(() -> new OffsetPageRequest(-1, 10, Sort.unsorted()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OffsetPageRequest(0, 0, Sort.unsorted()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
