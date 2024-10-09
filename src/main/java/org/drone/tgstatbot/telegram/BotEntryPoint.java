package org.drone.tgstatbot.telegram;

import lombok.extern.slf4j.Slf4j;
import org.drone.tgstatbot.config.BotConfig;
import org.drone.tgstatbot.service.AnswerGenerator;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class BotEntryPoint extends TelegramLongPollingBot {


    private final BotConfig botConfig;
    private final AnswerGenerator answerGenerator;
    private final ExecutorService executorService;

    public BotEntryPoint(BotConfig botConfig, AnswerGenerator answerGenerator, ExecutorService executorService) {
        super(botConfig.getToken());
        this.botConfig = botConfig;
        this.answerGenerator = answerGenerator;
        this.executorService = executorService;
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }

    @Override
    public void onUpdateReceived(Update update) {
        executorService.submit(() -> {
            if (update.hasMessage() && update.getMessage().hasText()) {
                log.info("message from {}, {}", update.getMessage().getChatId(), update.getMessage().getFrom().getUserName());
                String message = answerGenerator.answer(update);
                sendMessage(update.getMessage().getChatId(), message);
            }
        });
    }

    private void sendMessage(Long chatId, String textToSend) {
        log.info("answer to {}", chatId);

        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), textToSend);
        sendMessage.disableWebPagePreview();

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending message to {}", chatId, e);
        }
    }
}




