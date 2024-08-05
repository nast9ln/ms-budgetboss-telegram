package com.controller;

import com.config.BotConfig;
import com.service.impl.ExpenseService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Objects;

@Component
@AllArgsConstructor
@Slf4j
@Transactional
public class TelegramBot extends TelegramLongPollingBot {
    private final ExpenseService expenseService;

    private BotConfig botConfig;

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }


    @PostConstruct
    public void init() {
        expenseService.registerBot(this);
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();
        if (Objects.equals(messageText, "/yesterday")) {
            expenseService.getExpensesForYesterday(chatId);
        } else if (Objects.equals(messageText, "/month")) {
            expenseService.getExpensesForMonth(chatId);
        } else if (Objects.equals(messageText, "/daysmonth")) {
            expenseService.getExpensesForDaysOfMonth(chatId);
        } else if (Objects.equals(messageText, "/lastweek")) {
            expenseService.getExpensesForLastWeek(chatId);
        } else if (Objects.equals(messageText, "/salary")) {
            expenseService.getExpensesForSalary(chatId);
        } else if (messageText.contains("/")) {
            expenseService.getExpensesForCustomDays(chatId, messageText);
        } else {
            expenseService.processExpense(update);
        }
    }

    public void sendMessage(Long chatId, String text) {
        String chatIdStr = String.valueOf(chatId);
        SendMessage sendMessage = new SendMessage(chatIdStr, text);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Ошибка отправки сообщения", e);
        }
    }
}


