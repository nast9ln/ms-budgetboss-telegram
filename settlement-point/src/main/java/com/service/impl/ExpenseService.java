package com.service.impl;

import com.controller.TelegramBot;
import com.entity.Expense;
import com.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ExpenseService {
    private final ExpenseRepository expenseRepository;

    private TelegramBot telegramBot;

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+\\.\\d+)|(\\d+)|(\\d+\\,\\d+)");
    private static final Pattern TEXT_PATTERN = Pattern.compile("^([a-zA-Zа-яА-Я]+(?:\\s[a-zA-Zа-яА-Я]+)*(?:,\\s)?)+$");
    private static final String OUTPUT_PATTERN = "\nСумма: {0} рублей \nКатегория: {1} \n";
    private static final String DATE_PATTERN = "Дата : {0}\n";
    private static final String TOTAL_AMOUNT_PATTERN = "Итоговая сумма : {0} рублей";
    private static final String DAYS_AMOUNT_PATTERN = "\nИтого: {0} рублей";

    public void registerBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    public void processExpense(Update update) {
        String text = update.getMessage().getText();

        if (NUMBER_PATTERN.matcher(text).matches()) {
            text = text.replace(",", ".");
            Expense expense = new Expense();
            expense.setAmount(new BigDecimal(text));
            Instant date = Instant.ofEpochSecond(update.getMessage().getDate()).plus(3L, ChronoUnit.HOURS);
            if (update.getMessage().getForwardDate() != null)
                date = Instant.ofEpochSecond(update.getMessage().getForwardDate()).plus(3L, ChronoUnit.HOURS);
            expense.setDate(date);

            expenseRepository.save(expense);
        }

        if (TEXT_PATTERN.matcher(text).matches()) {
            Expense expense = expenseRepository.findLastExpense();
            expense.getCategory().add(text);
            expenseRepository.save(expense);
        }
    }

    public void getExpensesForYesterday(Long chatId) {
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        prepareAndSendResponse(chatId, yesterday);
    }

    public void getExpensesForLastWeek(Long chatId) {
        Instant lastWeek = Instant.now().minus(7, ChronoUnit.DAYS);
        prepareAndSendResponse(chatId, lastWeek);

    }

    public void getExpensesForMonth(Long chatId) {
        Instant lastMonth = Instant.now().minus(31, ChronoUnit.DAYS);
        prepareAndSendResponse(chatId, lastMonth);
    }


    public void getExpensesForDaysOfMonth(Long chatId) {
        int day = LocalDate.now().getDayOfMonth();
        Instant lastMonth = Instant.now().minus(day, ChronoUnit.DAYS);
        prepareAndSendResponse(chatId, lastMonth);

    }

    public void getExpensesForSalary(Long chatId) {
        LocalDate today = LocalDate.now();
        LocalDate start;

        if (today.getDayOfMonth() < 15) {
            start = today.withDayOfMonth(1).minusMonths(1).withDayOfMonth(15);
        } else {
            start = today.withDayOfMonth(15);
        }

        Instant startInst = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        prepareAndSendResponse(chatId, startInst);
    }

    public void getExpensesForCustomDays(Long chatId, String text) {
        int days = Integer.parseInt(text.replace("/", ""));
        Instant date = Instant.now().minus(days, ChronoUnit.DAYS);
        prepareAndSendResponse(chatId, date);

    }

    private void prepareAndSendResponse(Long chatId, Instant startDate) {
        Map<LocalDate, List<Expense>> groupedByDate = expenseRepository.findByDateBetween(startDate, Instant.now())
                .stream()
                .sorted(Comparator.comparing(Expense::getDate).reversed())
                .collect(Collectors.groupingBy(expense -> expense.getDate().atZone(ZoneId.systemDefault()).toLocalDate()));

        groupedByDate.forEach((date, exps) -> {
            StringBuilder message = new StringBuilder(MessageFormat.format(DATE_PATTERN, date));
            BigDecimal dayAmount = exps.stream()
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            exps.forEach(expense -> message.append(MessageFormat.format(OUTPUT_PATTERN, expense.getAmount(), expense.getCategory().toString().replace("[", "").replace("]", "").toLowerCase())));
            message.append(MessageFormat.format(DAYS_AMOUNT_PATTERN, dayAmount));
            telegramBot.sendMessage(chatId, message.toString());
        });

        BigDecimal totalAmount = groupedByDate.values()
                .stream()
                .flatMap(Collection::stream)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        telegramBot.sendMessage(chatId, MessageFormat.format(TOTAL_AMOUNT_PATTERN, totalAmount));
    }
}
