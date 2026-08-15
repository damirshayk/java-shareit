package ru.practicum.shareit.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {

    List<ItemRequest> findAllByRequestorIdOrderByCreatedDescIdDesc(Long requestorId);

    @Query(value = """
            SELECT *
            FROM requests
            WHERE requestor_id <> :userId
            ORDER BY created DESC, id DESC
            LIMIT :size OFFSET :from
            """, nativeQuery = true)
    List<ItemRequest> findAllByOtherUsers(
            @Param("userId") Long userId,
            @Param("from") int from,
            @Param("size") int size
    );
}
