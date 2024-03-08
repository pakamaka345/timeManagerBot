package com.umcs.lessons;


import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class timeManagerBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(timeManagerBot.class);
    private static final Dotenv dotenv = Dotenv.load();
    private Map<Long, DayHandler> dayHandlers = new HashMap<>();
    private static final Pattern TIME_PATTERN = Pattern.compile("^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$");
    private static final String INVALID_TIME_FORMAT = "Неправильний формат часу. Введи час у форматі хх:хх. Будь ласка, спробуй ще раз. \uD83D\uDE14";
    private String day;
    private boolean isPlan;
    private IsAction isAction;
    private Plan plan;
    @Override
    public void onUpdateReceived(Update update){
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();
            long userId = update.getMessage().getFrom().getId();

            DayHandler dayHandler = dayHandlers.get(userId);
            if (dayHandler == null){
                dayHandler = new DayHandler();
                dayHandlers.put(userId, dayHandler);
            }

            // /start command - start the bot
            if (messageText.equals("/start")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("*Хелоу* ✌️\n" +
                        "\n" +
                        "Я твій персональний бот для допомоги тобі в складанні особистого плану на тиждень \uD83D\uDE0E\n" +
                        "\n" +
                        "_Обери дію яку бажаєш виконати_ ⬇️\n");
                sendMessage.setParseMode("Markdown");

                sendMessage.setReplyMarkup(createInlineKeyboardMarkupForStart());
                try {
                    execute(sendMessage);
                } catch (Exception e) {
                    logger.info("An error occurred", e);
                }
            }

            if (isPlan && isAction == IsAction.NAME){
                logger.info("isPlan = " + isPlan + " isAction = " + isAction + " messageText = " + messageText + " day = " + day);
                plan.setPlanName(messageText);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("\uD83D\uDD70\uFE0F Вкажи початок (вкажи час у форматі хх:хх)");
                isAction = IsAction.TIME_START;
                try {
                    execute(sendMessage);
                } catch (Exception e){
                    logger.info("An error occurred", e);
                }
                return;
            }
            if (isPlan && isAction == IsAction.TIME_START){
                logger.info("isPlan = " + isPlan + " isAction = " + isAction + " messageText = " + messageText + " day = " + day);
                if(!TIME_PATTERN.matcher(messageText).matches()){
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(INVALID_TIME_FORMAT);
                    try {
                        execute(sendMessage);
                    } catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                    return;
                }
                LocalTime time = LocalTime.parse(messageText);
                plan.setStartTime(time);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("\uD83D\uDD70\uFE0F Вкажи кінець (вкажи час у форматі хх:хх) або _пропустити_ якщо невідомий час закінчення");
                sendMessage.setParseMode("Markdown");
                isAction = IsAction.TIME_END;
                try {
                    execute(sendMessage);
                } catch (Exception e){
                    logger.info("An error occurred", e);
                }
                return;
            }
            if (isPlan && isAction == IsAction.TIME_END){
                logger.info("isPlan = " + isPlan + " isAction = " + isAction + " messageText = " + messageText + " day = " + day);
                if(!TIME_PATTERN.matcher(messageText).matches() && !messageText.equals("пропустити")){
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(INVALID_TIME_FORMAT);
                    try {
                        execute(sendMessage);
                    } catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                    return;
                } else if (messageText.equals("пропустити")){
                    plan.setEndTime(null);
                    dayHandler.addPlanForDay(userId, day, plan);
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Супер! Це все чи додаємо ще план на цей день? \uD83E\uDD14");
                    sendMessage.setReplyMarkup(createInlineKeyboardMarkupForContinue());
                    isPlan = false;
                    isAction = null;
                    try {
                        execute(sendMessage);
                    } catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                    return;
                }
                LocalTime time = LocalTime.parse(messageText);
                if (time.isBefore(plan.getStartTime())){
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("В тебе план закінчився перед тим як він навіть почався. Задумайся над цим. \uD83D\uDE14");
                    try {
                        execute(sendMessage);
                    } catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                    return;
                }
                plan.setEndTime(time);
                dayHandler.addPlanForDay(userId, day, plan);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("Супер! Це все чи додаємо ще план на цей день? \uD83E\uDD14");
                sendMessage.setReplyMarkup(createInlineKeyboardMarkupForContinue());
                isPlan = false;
                isAction = null;
                try {
                    execute(sendMessage);
                } catch (Exception e){
                    logger.info("An error occurred", e);
                }
                return;
            }
            if (isAction == isAction.DELETE){
                logger.info("isPlan = " + isPlan + " isAction = " + isAction + " messageText = " + messageText + " day = " + day);
                PlanForDay planForDay = dayHandler.getPlansForDay(userId, day);
                if(planForDay != null && planForDay.getPlans().stream().anyMatch(plan -> plan.getPlanName().equals(messageText))){
                    dayHandler.deletePlanForDay(userId, day, messageText);
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("План успішно видалено!");

                    isAction = null;
                    try {
                        execute(sendMessage);
                    } catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                    printMenu(chatId);
                }
                else{
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Такого плану не існує. Спробуй ще раз.");
                    try {
                        execute(sendMessage);
                    } catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                }
            }
        }

        // We check if the update has a callback query (all button presses)
        if (update.hasCallbackQuery() && update.getCallbackQuery() != null){
            String call_data = update.getCallbackQuery().getData();
            long chatIdButton = update.getCallbackQuery().getMessage().getChatId();
            Long userId = update.getCallbackQuery().getFrom().getId();

            DayHandler dayHandler = dayHandlers.get(userId);
            if (dayHandler == null){
                dayHandler = new DayHandler();
                dayHandlers.put(userId, dayHandler);
            }

            // if button "create" is pressed
            if (call_data.equals("create")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                sendMessage.setText("Чудово! На який день ти бажаєш запланувати щось? \uD83E\uDD14");

                sendMessage.setReplyMarkup(createInlineKeyboardMarkupDays());
                try{
                    execute(sendMessage);
                }catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }
            // if button "menu" is pressed
            if (call_data.equals("menu")){
                printMenu(chatIdButton);
            }
            // if button "dayWeek" is pressed
            if (call_data.equals("monday") || call_data.equals("tuesday") || call_data.equals("wednesday") || call_data.equals("thursday") || call_data.equals("friday") || call_data.equals("saturday") || call_data.equals("sunday")){
                day = call_data;
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                SendMessage planMessage = new SendMessage();
                planMessage.setChatId(chatIdButton);
                switch (day){
                    case "monday": sendMessage.setText("Добре, ти обрав понеділок. Що робимо? \uD83D\uDE43"); break;
                    case "tuesday": sendMessage.setText("Добре, ти обрав вівторок. Що робимо? \uD83D\uDE43"); break;
                    case "wednesday": sendMessage.setText("Добре, ти обрав середу. Що робимо? \uD83D\uDE43"); break;
                    case "thursday": sendMessage.setText("Добре, ти обрав четвер. Що робимо? \uD83D\uDE43"); break;
                    case "friday": sendMessage.setText("Добре, ти обрав п'ятницю. Що робимо? \uD83D\uDE43"); break;
                    case "saturday": sendMessage.setText("Добре, ти обрав суботу. Що робимо? \uD83D\uDE43"); break;
                    case "sunday": sendMessage.setText("Добре, ти обрав неділю. Що робимо? \uD83D\uDE43"); break;
                }
                sendMessage.setReplyMarkup(createInlineKeyboardMarkupPlan());

                try{
                    execute(sendMessage);
                }catch (Exception e){
                    logger.info("An error occurred", e);
                }
                displayPlanForDay(dayHandler, day, planMessage, userId);
            }
            // if button "addPlan" is pressed
            if (call_data.equals("addPlan")){
                plan = new Plan();
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                sendMessage.setText("\uD83D\uDDC4\uFE0F Вкажи назву твого плану:");
                isPlan = true;
                isAction = IsAction.NAME;

                try{
                    execute(sendMessage);
                }catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }
            // if button "return to week choose" is pressed
            if (call_data.equals("backToWeek")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                sendMessage.setText("На який день ти бажаєш запланувати щось? \uD83E\uDD14");
                sendMessage.setReplyMarkup(createInlineKeyboardMarkupDays());
                try{
                    execute(sendMessage);
                }catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }
            // if button "deletePlan" is pressed
            if (call_data.equals("deletePlan")){
                PlanForDay planForDay = dayHandler.getPlansForDay(userId, day);
                if(planForDay != null && !planForDay.getPlans().isEmpty()){
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatIdButton);
                    sendMessage.setText("Обери план який ти хочеш видалити:");
                    isAction = IsAction.DELETE;
                    try{
                        execute(sendMessage);
                    }catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                }
                else {
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatIdButton);
                    sendMessage.setText("Ти ще не додав планів на цей день \uD83D\uDE14");
                    try{
                        execute(sendMessage);
                    }catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                }
            }
        }
    }

    // These methods are create InlineKeyboardMarkup
    private InlineKeyboardMarkup createInlineKeyboardMarkupForStart(){
        InlineKeyboardButton createButton = new InlineKeyboardButton();
        createButton.setText("1️⃣ СТВОРИТИ");
        createButton.setCallbackData("create");

        InlineKeyboardButton editButton = new InlineKeyboardButton();
        editButton.setText("2️⃣ РЕДАГУВАТИ");
        editButton.setCallbackData("edit");

        InlineKeyboardButton showButton = new InlineKeyboardButton();
        showButton.setText("3️⃣ ПОКАЗАТИ");
        showButton.setCallbackData("show");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(createButton);
        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        keyboardButtonsRow2.add(editButton);
        List<InlineKeyboardButton> keyboardButtonsRow3 = new ArrayList<>();
        keyboardButtonsRow3.add(showButton);


        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow1);
        rowList.add(keyboardButtonsRow2);
        rowList.add(keyboardButtonsRow3);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }
    private InlineKeyboardMarkup createInlineKeyboardMarkupDays(){
        InlineKeyboardButton mondayButton = new InlineKeyboardButton();
        mondayButton.setText("\uD83D\uDDD3\uFE0F ПОНЕДІЛОК");
        mondayButton.setCallbackData("monday");

        InlineKeyboardButton tuesdayButton = new InlineKeyboardButton();
        tuesdayButton.setText("\uD83D\uDDD3\uFE0F ВІВТОРОК");
        tuesdayButton.setCallbackData("tuesday");

        InlineKeyboardButton wednesdayButton = new InlineKeyboardButton();
        wednesdayButton.setText("\uD83D\uDDD3\uFE0F СЕРЕДА");
        wednesdayButton.setCallbackData("wednesday");

        InlineKeyboardButton thursdayButton = new InlineKeyboardButton();
        thursdayButton.setText("\uD83D\uDDD3\uFE0F ЧЕТВЕР");
        thursdayButton.setCallbackData("thursday");

        InlineKeyboardButton fridayButton = new InlineKeyboardButton();
        fridayButton.setText("\uD83D\uDDD3\uFE0F П'ЯТНИЦЯ");
        fridayButton.setCallbackData("friday");

        InlineKeyboardButton saturdayButton = new InlineKeyboardButton();
        saturdayButton.setText("\uD83D\uDDD3\uFE0F СУБОТА");
        saturdayButton.setCallbackData("saturday");

        InlineKeyboardButton sundayButton = new InlineKeyboardButton();
        sundayButton.setText("\uD83D\uDDD3\uFE0F НЕДІЛЯ");
        sundayButton.setCallbackData("sunday");

        InlineKeyboardButton menuButton = new InlineKeyboardButton();
        menuButton.setText("\uD83D\uDDC4\uFE0F Меню");
        menuButton.setCallbackData("menu");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(mondayButton);

        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        keyboardButtonsRow2.add(tuesdayButton);

        List<InlineKeyboardButton> keyboardButtonsRow3 = new ArrayList<>();
        keyboardButtonsRow3.add(wednesdayButton);

        List<InlineKeyboardButton> keyboardButtonsRow4 = new ArrayList<>();
        keyboardButtonsRow4.add(thursdayButton);

        List<InlineKeyboardButton> keyboardButtonsRow5 = new ArrayList<>();
        keyboardButtonsRow5.add(fridayButton);

        List<InlineKeyboardButton> keyboardButtonsRow6 = new ArrayList<>();
        keyboardButtonsRow6.add(saturdayButton);

        List<InlineKeyboardButton> keyboardButtonsRow7 = new ArrayList<>();
        keyboardButtonsRow7.add(sundayButton);

        List<InlineKeyboardButton> keyboardButtonsRow8 = new ArrayList<>();
        keyboardButtonsRow8.add(menuButton);

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(keyboardButtonsRow1);
        rowList.add(keyboardButtonsRow2);
        rowList.add(keyboardButtonsRow3);
        rowList.add(keyboardButtonsRow4);
        rowList.add(keyboardButtonsRow5);
        rowList.add(keyboardButtonsRow6);
        rowList.add(keyboardButtonsRow7);
        rowList.add(keyboardButtonsRow8);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }
    private InlineKeyboardMarkup createInlineKeyboardMarkupPlan(){
        InlineKeyboardButton addPlanButton = new InlineKeyboardButton();
        addPlanButton.setText("➕ ДОДАТИ ПЛАН");
        addPlanButton.setCallbackData("addPlan");

        InlineKeyboardButton deletePlanButton = new InlineKeyboardButton();
        deletePlanButton.setText("➖ ПРИБРАТИ ПЛАН");
        deletePlanButton.setCallbackData("deletePlan");

        InlineKeyboardButton weekButton = new InlineKeyboardButton();
        weekButton.setText("\uD83D\uDDD3\uFE0F ВИБРАТИ ІНШИЙ ДЕНЬ");
        weekButton.setCallbackData("backToWeek");

        InlineKeyboardButton menuButton = new InlineKeyboardButton();
        menuButton.setText("\uD83D\uDDC4\uFE0F Меню");
        menuButton.setCallbackData("menu");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(addPlanButton);

        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        keyboardButtonsRow2.add(deletePlanButton);

        List<InlineKeyboardButton> keyboardButtonsRow3 = new ArrayList<>();
        keyboardButtonsRow3.add(weekButton);

        List<InlineKeyboardButton> keyboardButtonsRow4 = new ArrayList<>();
        keyboardButtonsRow4.add(menuButton);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(List.of(keyboardButtonsRow1, keyboardButtonsRow2, keyboardButtonsRow3, keyboardButtonsRow4));

        return inlineKeyboardMarkup;
    }
    private InlineKeyboardMarkup createInlineKeyboardMarkupForContinue(){
        InlineKeyboardButton continueButton = new InlineKeyboardButton();
        continueButton.setText("➕ ЩЕ ДОДАТИ");
        continueButton.setCallbackData("addPlan");

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(continueButton);

        InlineKeyboardButton endButton = new InlineKeyboardButton();
        endButton.setText("✅ ЦЕ ВСЕ!");
        endButton.setCallbackData("menu");

        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        keyboardButtonsRow2.add(endButton);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(List.of(keyboardButtonsRow1, keyboardButtonsRow2));

        return inlineKeyboardMarkup;
    }

    // These methods are display something
    private void displayPlanForDay(DayHandler dayHandler, String day, SendMessage sendMessage, long userId){
        PlanForDay planForDay = dayHandler.getPlansForDay(userId, day);
        if(planForDay == null || planForDay.getPlans().isEmpty()){
            sendMessage.setText("Ти ще не додав планів на цей день \uD83D\uDE14");
        }
        else{
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Твій план:\n");
            for(Plan plan : planForDay.getPlans()){
                if(plan.getEndTime() == null){
                    stringBuilder.append(plan.getStartTime() + " " + plan.getPlanName() + "\n");
                }else{
                    stringBuilder.append(plan.getStartTime() + " - " + plan.getEndTime() + " " + plan.getPlanName() + "\n");
                }
            }
            sendMessage.setText(stringBuilder.toString());
        }
        try {
            execute(sendMessage);
        } catch (Exception e) {
            logger.info("An error occurred", e);
        }
    }
    void printMenu(long chatId){
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Обери дію яку хочеш виконати ⬇️");
        sendMessage.setParseMode("Markdown");

        sendMessage.setReplyMarkup(createInlineKeyboardMarkupForStart());
        try {
            execute(sendMessage);
        } catch (Exception e) {
            logger.info("An error occurred", e);
        }
    }

    // This methods must always return your Bot username and token
    @Override
    public String getBotUsername(){
        return "tImE_MaNaGeR_informatics_bot";
    }
    @Override
    public String getBotToken(){
        return dotenv.get("BOT_TOKEN");
    }
}
