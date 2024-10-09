package org.drone.tgstatbot.repository;

import org.drone.tgstatbot.model.Percentage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PercentageRepository extends JpaRepository<Percentage, Long> {
}
