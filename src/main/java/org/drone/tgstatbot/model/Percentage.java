package org.drone.tgstatbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "percentage")
@AllArgsConstructor
@NoArgsConstructor
public class Percentage {

    @Id
    private long chatId;
    private short percentage;
}
