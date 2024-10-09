package org.drone.tgstatbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "chat_state")
@AllArgsConstructor
@NoArgsConstructor
public class ChatState {
    @Id
    private long chatId;
    private String chatState;
}
