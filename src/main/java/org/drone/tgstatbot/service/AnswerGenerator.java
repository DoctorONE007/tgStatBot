package org.drone.tgstatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.drone.tgstatbot.dao.MessageState;
import org.drone.tgstatbot.dao.TgStatData;
import org.jsoup.HttpStatusException;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerGenerator {

    private static final String START_TEXT = """
            Введите текст каждого канала в одну строчку в формате:
            <Название канала> https://t.me/<id канала> <Cумма>
            \s
            Если хотите ввести несколько каналов для рассчета, вводите каждое с новой строчки
            \s
            Пример:
            Двоичный кот https://t.me/binarcat 1к
            Двоичный кот https://t.me/binarcat от 1000
            https://t.me/binarcat от 1к
            https://t.me/binarcat 1000
            \s""";

    private static final String DEFAULT_PERCENTAGE = "Сумма последовательно увеличивается на 9%, 15%, 5% и 20%.";
    private static final String PERCENTAGE_RESET = "Установлено значение процента по умолчанию.";
    private static final String PERCENTAGE_SET = "Введите целое значение процента на которое нужно увеличить сумму. От 1 до 1000.";
    private static final String PERCENTAGE_ERROR = "Ошибка формата ввода.";
    private static final String PERCENTAGE_SUCCESS = "Установлено значение:";
    private static final String PERCENTAGE_INCREASE = "Сумма увеличивается на";

    private static final Pattern linkPattern = Pattern.compile("https://t\\.me/\\S+");
    private static final Pattern pricePattern = Pattern.compile(" \\d* ?\\d*$|\\d*к$|\\d*k$");
    private static final Pattern percentagePattern = Pattern.compile("^\\d+");

    private static final NumberFormat formatter = NumberFormat.getInstance(Locale.of("ru"));

    private final DbService dbService;


    private final TgStatParser tgStatParser;

    public String answer(Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        if (text.equals("/start") || text.equals("/get_percentage") || text.equals("/reset_percentage") || text.equals("/set_percentage")) {
            if (dbService.getChatStateByChatId(chatId) == MessageState.PERCENTAGE_IN_PROGRESS) {
                dbService.deleteChatStateByChatId(chatId);
            }
        }

        if (text.equals("/start")) {
            return START_TEXT;
        } else if (text.equals("/get_percentage")) {
            return getPercentage(chatId);
        } else if (text.equals("/reset_percentage")) {
            return resetPercentage(chatId);
        } else if (text.equals("/set_percentage")) {
            return setPercentageFirstStep(chatId);
        } else if (dbService.getChatStateByChatId(chatId) == MessageState.PERCENTAGE_IN_PROGRESS) {
            return setPercentageSecondStep(chatId, text);
        } else {
            return validateAndCallParser(text, chatId);
        }
    }

    private String getPercentage(Long chatId) {
        Short percentage = dbService.getPercentageByChatId(chatId);
        return percentage == null ? DEFAULT_PERCENTAGE : PERCENTAGE_INCREASE + " " + percentage + "%";
    }

    private String resetPercentage(Long chatId) {
        dbService.deletePercentageByChatId(chatId);
        return PERCENTAGE_RESET;
    }

    private String setPercentageFirstStep(Long chatId) {
        dbService.saveChatState(chatId, MessageState.PERCENTAGE_IN_PROGRESS);
        return PERCENTAGE_SET;
    }

    private String setPercentageSecondStep(Long chatId, String text) {
        Matcher percentageMatcher = percentagePattern.matcher(text);
        if (percentageMatcher.find()) {
            short percentage;
            String percentageStr = percentageMatcher.group();
            try {
                percentage = Short.parseShort(percentageStr);
            } catch (NumberFormatException exception) {
                log.error("NumberFormatException for {}", percentageStr);
                return PERCENTAGE_ERROR + "\n" + PERCENTAGE_SET;
            }
            if (percentage < 1 || percentage > 1000) {
                log.error("Wrong percentage: {}", percentage);
                return PERCENTAGE_ERROR + "\n" + PERCENTAGE_SET;
            }
            dbService.savePercentageByChatId(chatId, percentage);
            dbService.deleteChatStateByChatId(chatId);
            return PERCENTAGE_SUCCESS + " " + percentage + "%";
        } else {
            log.error("Error parsing: {}", text);
            return PERCENTAGE_ERROR + "\n" + PERCENTAGE_SET;
        }
    }

    private String generateFormatErrorMessage(String line) {
        return "Не удалось обработать сообщение\n" +
                line + "\n" +
                "Ошибка форматирования";
    }

    private String generateResponseErrorMessage(String line) {
        return "Не удалось обработать сообщение\n" +
                line + "\n" +
                "По введенным данным не найдено информации или сервис не доступен";
    }

    private String validateAndCallParser(String text, Long chatId) {
        text = text.trim().replaceAll(" +", " ");

        List<String> splittedText = Arrays.stream(text.split("\\r?\\n")).filter(line -> !line.trim().isEmpty()).toList();
        List<String> answers = new ArrayList<>();
        splittedText.forEach(line -> {
            Matcher linkMatcher = linkPattern.matcher(line);
            if (linkMatcher.find()) {
                String link = linkMatcher.group();
                String channelName = link.replace("https://t.me/", "");
                TgStatData tgStatData;
                try {
                    tgStatData = tgStatParser.getResponse(channelName);
                    if (tgStatData == null) {
                        log.error("null for {}", line);
                        answers.add(generateResponseErrorMessage(line));
                    } else {
                        answers.add(generateSuccessMessage(tgStatData, line, link, chatId));
                    }
                } catch (HttpStatusException e) {
                    log.error("HttpStatusException for {}", line);
                    answers.add(generateResponseErrorMessage(line));
                }

            } else {
                answers.add(generateFormatErrorMessage(line));
            }
        });
        return String.join("\n\n", answers);
    }

    private String generateSuccessMessage(TgStatData tgStatData, String line, String link, Long chatId) {
        Integer countedPrice = countPrice(line, chatId);

        return tgStatData.getName() + "\n" + link + "\n" + "Подписчики: " + tgStatData.getSubscribers() + "\n" + "Охват: " +
                tgStatData.getCoverage() + (countedPrice == null ? "" : "\n" + "Цена: от " + formatter.format(countedPrice));

    }

    private Integer countPrice(String line, Long chatId) {
        String priceStr;
        Matcher priceMatcher = pricePattern.matcher(line);
        if (!priceMatcher.find()) {
            return null;
        }
        priceStr = priceMatcher.group().trim().replaceAll("\\s+","");
        if (priceStr.contains("k") || priceStr.contains("к")) {
            priceStr = priceStr.replace("k", "000").replace("к", "000");
        }
        int price = Integer.parseInt(priceStr);
        Short percentage = dbService.getPercentageByChatId(chatId);
        if (percentage != null) {
            price = (int) (price * (1 + (double) percentage / 100));
        } else {
            price = (int) (price * 1.09 * 1.15 * 1.05 * 1.2);
        }
        return price;
    }
}
