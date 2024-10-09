package org.drone.tgstatbot.repository;

import org.drone.tgstatbot.model.ChatState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatStateRepository extends JpaRepository<ChatState, Long> {
}
