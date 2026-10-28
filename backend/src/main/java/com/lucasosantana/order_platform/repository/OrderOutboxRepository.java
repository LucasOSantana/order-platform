package com.lucasosantana.order_platform.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lucasosantana.order_platform.model.OrderOutbox;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {

	@Query(value = """
			SELECT * FROM order_outbox
			WHERE published_at IS NULL
			ORDER BY created_at
			LIMIT :limit
			FOR UPDATE SKIP LOCKED
			""", nativeQuery = true)
	List<OrderOutbox> lockPending(@Param("limit") int limit);

}
