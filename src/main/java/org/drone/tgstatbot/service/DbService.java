package org.drone.tgstatbot.service;

import lombok.RequiredArgsConstructor;
import org.drone.tgstatbot.dao.MessageState;
import org.drone.tgstatbot.model.ChatState;
import org.drone.tgstatbot.model.Percentage;
import org.drone.tgstatbot.repository.ChatStateRepository;
import org.drone.tgstatbot.repository.PercentageRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbService {

    private final ChatStateRepository chatStateRepository;
    private final PercentageRepository percentageRepository;

    public MessageState getChatStateByChatId(long chatId){
        ChatState chatState = chatStateRepository.findById(chatId).orElse(null);
        if (chatState == null){
            return null;
        }
        return MessageState.valueOf(chatState.getChatState());
    }
    public void deleteChatStateByChatId(long chatId){
        chatStateRepository.deleteById(chatId);
    }

    public void saveChatState(long chatId, MessageState messageState){
        chatStateRepository.save(new ChatState(chatId,messageState.name()));
    }

    public Short getPercentageByChatId(long chatId){
        Percentage percentage = percentageRepository.findById(chatId).orElse(null);
        if(percentage == null){
            return null;
        }
        return percentage.getPercentage();
    }

    public void deletePercentageByChatId(long chatId){
        percentageRepository.deleteById(chatId);
    }

    public void savePercentageByChatId(long chatId, short percentage){
        percentageRepository.save(new Percentage(chatId, percentage));
    }
}
