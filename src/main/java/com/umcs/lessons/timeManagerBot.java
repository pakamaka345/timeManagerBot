package com.umcs.lessons;


import com.umcs.lessons.actions.IsAction;
import com.umcs.lessons.actions.IsStart;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class timeManagerBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(timeManagerBot.class);
    private static final Dotenv dotenv = Dotenv.load();
    private Map<Long, DayHandler> dayHandlers = new HashMap<>();
    private Map<Long, Timer> chatTimers = new HashMap<>();
    private final long delay = 240 * 1000;
    private static final Pattern TIME_PATTERN = Pattern.compile("^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$");
    private static final String INVALID_TIME_FORMAT = "Неправильний формат часу. Введи час у форматі хх:хх. Будь ласка, спробуй ще раз. \uD83D\uDE14";
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
                logger.info("userId = " + userId + " chatId = " + chatId);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("*Хелоу* ✌️\n" +
                        "\n" +
                        "Я твій персональний бот для допомоги тобі в складанні особистого плану на тиждень \uD83D\uDE0E\n" +
                        "\n" +
                        "_Обери дію яку бажаєш виконати_ ⬇️\n");
                sendMessage.setParseMode("Markdown");
                dayHandler.setIsStart(IsStart.WAIT);
                sendMessage.setReplyMarkup(createInlineKeyboardMarkupForStart());
                try {
                    execute(sendMessage);
                } catch (Exception e) {
                    logger.info("An error occurred", e);
                }
            }

            if (chatTimers.get(chatId) == null){
                return;
            }

            if (dayHandler.getIsAction() == IsAction.NAME){
                logger.info(" isAction = " + dayHandler.getIsAction() + " messageText = " + messageText + " day = " + dayHandler.getDay());
                Plan plan = dayHandler.getPlan();
                PlanForDay planForDay = dayHandler.getPlansForDay(userId);
                if(planForDay != null && planForDay.getPlans().stream().anyMatch(p -> p.getPlanName().equals(messageText))){
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Такий план вже існує. Спробуй ще раз.");
                    try {
                        execute(sendMessage);
                    } catch (Exception e) {
                        logger.info("An error occurred", e);
                    }
                    return;
                }
                plan.setPlanName(messageText);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("\uD83D\uDD70\uFE0F Вкажи початок (вкажи час у форматі хх:хх)");
                dayHandler.setIsAction(IsAction.TIME_START);
                try {
                    execute(sendMessage);
                } catch (Exception e){
                    logger.info("An error occurred", e);
                }
                return;
            }
            if (dayHandler.getIsAction() == IsAction.TIME_START){
                logger.info(" isAction = " + dayHandler.getIsAction() + " messageText = " + messageText + " day = " + dayHandler.getDay());
                Plan plan = dayHandler.getPlan();
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
                dayHandler.setIsAction(IsAction.TIME_END);
                try {
                    execute(sendMessage);
                } catch (Exception e){
                    logger.info("An error occurred", e);
                }
                return;
            }
            if (dayHandler.getIsAction() == IsAction.TIME_END){
                logger.info(" isAction = " + dayHandler.getIsAction() + " messageText = " + messageText + " day = " + dayHandler.getDay());
                Plan plan = dayHandler.getPlan();
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
                    plan.setId(UUID.randomUUID().toString());
                    dayHandler.addPlanForDay(userId, plan);
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Супер! Це все чи додаємо ще план на цей день? \uD83E\uDD14");
                    sendMessage.setReplyMarkup(createInlineKeyboardMarkupForContinue());
                    dayHandler.setIsAction(null);
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
                    sendMessage.setText("В тебе план закінчився перед тим як він навіть почався. Задумайся над цим.(Введи ще раз) \uD83D\uDE14");
                    try {
                        execute(sendMessage);
                    } catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                    return;
                }
                plan.setEndTime(time);
                dayHandler.addPlanForDay(userId, plan);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("Супер! Це все чи додаємо ще план на цей день? \uD83E\uDD14");
                sendMessage.setReplyMarkup(createInlineKeyboardMarkupForContinue());
                dayHandler.setIsAction(null);
                try {
                    execute(sendMessage);
                } catch (Exception e){
                    logger.info("An error occurred", e);
                }
                return;
            }
            if (dayHandler.getIsAction() == IsAction.DELETE){
                logger.info(" isAction = " + dayHandler.getIsAction() + " messageText = " + messageText + " day = " + dayHandler);
                PlanForDay planForDay = dayHandler.getPlansForDay(userId);
                if(planForDay != null && planForDay.getPlans().stream().anyMatch(plan -> plan.getPlanName().equals(messageText))){
                    dayHandler.deletePlanForDay(userId, messageText);
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("План успішно видалено!");

                    dayHandler.setIsAction(null);
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
            if (dayHandler.getIsAction() == IsAction.EDIT){
                logger.info(" isAction = " + dayHandler.getIsAction() + " messageText = " + messageText + " day = " + dayHandler);
                PlanForDay planForDay = dayHandler.getPlansForDay(userId);
                if(planForDay != null && planForDay.getPlans().stream().anyMatch(plan -> plan.getPlanName().equals(messageText))){
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Вибере що хочеш редагувати. \uD83E\uDD14");
                    dayHandler.setPlan(planForDay.getPlans().stream().filter(plan -> plan.getPlanName().equals(messageText)).findFirst().get());
                    sendMessage.setReplyMarkup(createInlineKeyboardMarkupForEdit());

                    try {
                        execute(sendMessage);
                    } catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                }
            }
            if (dayHandler.getIsAction() == IsAction.EDIT_NAME){
                logger.info(" isAction = " + dayHandler.getIsAction() + " messageText = " + messageText + " day = " + dayHandler);
                PlanForDay planForDay = dayHandler.getPlansForDay(userId);
                Plan plan = dayHandler.getPlan();
                planForDay.editName(messageText, plan);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("Дякую. Зміни внесено ✅");
                sendMessage.setReplyMarkup(createInlineKeyboardMarkupForEndEdit());
                dayHandler.setIsAction(null);
                try {
                    execute(sendMessage);
                } catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }
            if (dayHandler.getIsAction() == IsAction.EDIT_START_TIME){
                logger.info(" isAction = " + dayHandler.getIsAction() + " messageText = " + messageText + " day = " + dayHandler);
                PlanForDay planForDay = dayHandler.getPlansForDay(userId);
                Plan plan = dayHandler.getPlan();
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
                planForDay.editStartTime(time, plan);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("Дякую. Зміни внесено ✅");
                sendMessage.setReplyMarkup(createInlineKeyboardMarkupForEndEdit());
                dayHandler.setIsAction(null);
                try {
                    execute(sendMessage);
                } catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }
            if (dayHandler.getIsAction() == IsAction.EDIT_END_TIME){
                logger.info(" isAction = " + dayHandler.getIsAction() + " messageText = " + messageText + " day = " + dayHandler);
                PlanForDay planForDay = dayHandler.getPlansForDay(userId);
                Plan plan = dayHandler.getPlan();
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
                    planForDay.editEndTime(null, plan);
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("Дякую. Зміни внесено ✅");
                    sendMessage.setReplyMarkup(createInlineKeyboardMarkupForEndEdit());
                    dayHandler.setIsAction(null);
                    try {
                        execute(sendMessage);
                    } catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                    return;
                }
                LocalTime time = LocalTime.parse(messageText);
                if (time.isBefore(planForDay.getPlans().stream().filter(p -> p.equals(plan)).findFirst().get().getStartTime())){
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText("В тебе план закінчився перед тим як він навіть почався. Задумайся над цим.(Введи ще раз) \uD83D\uDE14");
                    try {
                        execute(sendMessage);
                    } catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                    return;
                }
                planForDay.editEndTime(time, plan);
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                sendMessage.setText("Дякую. Зміни внесено ✅");
                sendMessage.setReplyMarkup(createInlineKeyboardMarkupForEndEdit());
                dayHandler.setIsAction(null);
                try {
                    execute(sendMessage);
                } catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }


            if (messageText.toLowerCase().equals("так") || messageText.toLowerCase().equals("ні")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatId);
                if (messageText.toLowerCase().equals("так")) {
                    sendMessage.setText("О дякую велике");
                } else {
                    sendMessage.setText("Жаль");
                }
                try {
                    execute(sendMessage);
                } catch (Exception e) {
                    logger.info("An error occurred", e);
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
                dayHandler.setIsStart(IsStart.CREATE);
                try{
                    execute(sendMessage);
                }catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }
            // if button "menu" is pressed
            if (call_data.equals("menu")){
                dayHandler.setIsStart(IsStart.WAIT);
                printMenu(chatIdButton);
            }
            // if button "dayWeek" is pressed
            if (call_data.equals("monday") || call_data.equals("tuesday") || call_data.equals("wednesday") || call_data.equals("thursday") || call_data.equals("friday") || call_data.equals("saturday") || call_data.equals("sunday")){
                logger.info(" isStart = " + dayHandler.getIsStart() + " call_data = " + call_data);

                if(dayHandler.getIsStart() == IsStart.CREATE){
                    dayHandler.setDay(call_data);
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatIdButton);
                    SendMessage planMessage = new SendMessage();
                    planMessage.setChatId(chatIdButton);
                    sendMessage.setText("Добре, ти обрав " + getDayOfWeek(call_data) + ". Який план ти хочеш додати? (введи назву плану) \uD83E\uDD14");
                    sendMessage.setReplyMarkup(createInlineKeyboardMarkupPlan());

                    try{
                        execute(sendMessage);
                    }catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                    displayPlanForDay(dayHandler, planMessage, userId);
                } else if (dayHandler.getIsStart() == IsStart.EDIT){
                    dayHandler.setDay(call_data);
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatIdButton);
                    sendMessage.setText("Добре ти обрав " + getDayOfWeek(call_data) + ". Який план ти хочеш редагувати? (введи назву плану) \uD83E\uDD14");
                    try{
                        execute(sendMessage);
                    }catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                    displayPlanForDay(dayHandler, sendMessage, userId);
                } else if (dayHandler.getIsStart() == IsStart.SHOW){
                    dayHandler.setDay(call_data);
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatIdButton);
                    sendMessage.setText("Твій план на " + getDayOfWeek(call_data) + " \uD83D\uDE07");
                    try {
                        execute(sendMessage);
                    } catch (Exception e){
                        logger.info("An error occurred", e);
                    }
                    displayPlanForDay(dayHandler, sendMessage, userId);
                    printMenu(chatIdButton);
                    dayHandler.setIsStart(null);
                }
            }
            // if button "addPlan" is pressed
            if (call_data.equals("addPlan")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                sendMessage.setText("\uD83D\uDDC4\uFE0F Вкажи назву твого плану:");
                dayHandler.setPlan(new Plan());
                dayHandler.setIsAction(IsAction.NAME);
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
                PlanForDay planForDay = dayHandler.getPlansForDay(userId);
                if(planForDay != null && !planForDay.getPlans().isEmpty()){
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatIdButton);
                    sendMessage.setText("Обери план який ти хочеш видалити (назва плану):");
                    dayHandler.setIsAction(IsAction.DELETE);
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


            // if button "edit" is pressed
            if (call_data.equals("edit")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                sendMessage.setText("На який день ти бажаєш редагувати плани? \uD83E\uDD14");

                sendMessage.setReplyMarkup(createInlineKeyboardMarkupDays());
                dayHandler.setIsStart(IsStart.EDIT);
                dayHandler.setIsAction(IsAction.EDIT);

                try{
                    execute(sendMessage);
                }catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }
            if (call_data.equals("editName")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                sendMessage.setText("Введи нову назву плану:");
                dayHandler.setIsAction(IsAction.EDIT_NAME);
                try{
                    execute(sendMessage);
                }catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }
            if (call_data.equals("editTimeStart")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                sendMessage.setText("Введи новий час початку плану (у форматі хх:хх):");
                dayHandler.setIsAction(IsAction.EDIT_START_TIME);
                try{
                    execute(sendMessage);
                }catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }
            if (call_data.equals("editTimeEnd")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                sendMessage.setText("Введи новий час закінчення плану (у форматі хх:хх) або _пропустити_ якщо невідомий час закінчення:");
                sendMessage.setParseMode("Markdown");
                dayHandler.setIsAction(IsAction.EDIT_END_TIME);
                try{
                    execute(sendMessage);
                }catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }


            // if button "show" is pressed
            if (call_data.equals("show")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                sendMessage.setText("Що плануєш переглянути? ⬇ \uFE0F");
                sendMessage.setReplyMarkup(createInlineKeyboardMarkupForShow());
                try{
                    execute(sendMessage);
                } catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }
            if (call_data.equals("showWeek")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                displayPlanForWeek(dayHandler, sendMessage, userId);
            }
            if (call_data.equals("showDay")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                sendMessage.setText("Обери день для перегляду ⬇\uFE0F");
                sendMessage.setReplyMarkup(createInlineKeyboardMarkupDays());
                dayHandler.setIsStart(IsStart.SHOW);
                try {
                    execute(sendMessage);
                } catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }
            if (call_data.equals("showNearestPlan")){
                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                displayNearestPlan(dayHandler, sendMessage, userId);
            }

            if (call_data.equals("random")){
                List<String> randomMessages = Arrays.asList("Так звісно йти на пари \uD83D\uDC4D", "Звісно не йти. Кому ті пари потрібні \uD83E\uDD2E");

                SendMessage sendMessage = new SendMessage();
                sendMessage.setChatId(chatIdButton);
                sendMessage.setText(randomMessages.get(new Random().nextInt(randomMessages.size())));
                try {
                    execute(sendMessage);
                } catch (Exception e){
                    logger.info("An error occurred", e);
                }
            }

            resetTimer(chatIdButton);
        }
    }

    // These methods are create InlineKeyboardMarkup
    private InlineKeyboardButton createInlineKeyboardButton(String text, String callbackData){
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
    private List<InlineKeyboardButton> createKeyboardRow(InlineKeyboardButton... buttons){
        return new ArrayList<>(Arrays.asList(buttons));
    }
    private InlineKeyboardMarkup createInlineKeyboardMarkupForStart(){
        InlineKeyboardButton createButton = createInlineKeyboardButton("1️⃣ СТВОРИТИ/ВИДАЛИТИ", "create");
        InlineKeyboardButton editButton = createInlineKeyboardButton("2️⃣ РЕДАГУВАТИ", "edit");
        InlineKeyboardButton showButton = createInlineKeyboardButton("3️⃣ ПОКАЗАТИ", "show");

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(createKeyboardRow(createButton));
        rowList.add(createKeyboardRow(editButton));
        rowList.add(createKeyboardRow(showButton));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }
    private InlineKeyboardMarkup createInlineKeyboardMarkupDays(){
        InlineKeyboardButton mondayButton = createInlineKeyboardButton("\uD83D\uDDD3\uFE0F ПОНЕДІЛОК", "monday");
        InlineKeyboardButton tuesdayButton = createInlineKeyboardButton("\uD83D\uDDD3\uFE0F ВІВТОРОК", "tuesday");
        InlineKeyboardButton wednesdayButton = createInlineKeyboardButton("\uD83D\uDDD3\uFE0F СЕРЕДА", "wednesday");
        InlineKeyboardButton thursdayButton = createInlineKeyboardButton("\uD83D\uDDD3\uFE0F ЧЕТВЕРГ", "thursday");
        InlineKeyboardButton fridayButton = createInlineKeyboardButton("\uD83D\uDDD3\uFE0F ПʼЯТНИЦЯ", "friday");
        InlineKeyboardButton saturdayButton = createInlineKeyboardButton("\uD83D\uDDD3\uFE0F СУБОТА", "saturday");
        InlineKeyboardButton sundayButton = createInlineKeyboardButton("\uD83D\uDDD3\uFE0F НЕДІЛЯ", "sunday");
        InlineKeyboardButton menuButton = createInlineKeyboardButton("\uD83D\uDDC4\uFE0F МЕНЮ", "menu");

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(createKeyboardRow(mondayButton));
        rowList.add(createKeyboardRow(tuesdayButton));
        rowList.add(createKeyboardRow(wednesdayButton));
        rowList.add(createKeyboardRow(thursdayButton));
        rowList.add(createKeyboardRow(fridayButton));
        rowList.add(createKeyboardRow(saturdayButton));
        rowList.add(createKeyboardRow(sundayButton));
        rowList.add(createKeyboardRow(menuButton));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }
    private InlineKeyboardMarkup createInlineKeyboardMarkupPlan(){
        InlineKeyboardButton addPlanButton = createInlineKeyboardButton("➕ ДОДАТИ ПЛАН", "addPlan");
        InlineKeyboardButton deletePlanButton = createInlineKeyboardButton("➖ ПРИБРАТИ ПЛАН", "deletePlan");
        InlineKeyboardButton weekButton = createInlineKeyboardButton("\uD83D\uDDD3\uFE0F ВИБРАТИ ІНШИЙ ДЕНЬ", "backToWeek");
        InlineKeyboardButton menuButton = createInlineKeyboardButton("\uD83D\uDDC4\uFE0F МЕНЮ", "menu");

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(createKeyboardRow(addPlanButton));
        rowList.add(createKeyboardRow(deletePlanButton));
        rowList.add(createKeyboardRow(weekButton));
        rowList.add(createKeyboardRow(menuButton));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }
    private InlineKeyboardMarkup createInlineKeyboardMarkupForContinue(){
        InlineKeyboardButton continueButton = createInlineKeyboardButton("➕ ЩЕ ДОДАТИ", "addPlan");
        InlineKeyboardButton endButton = createInlineKeyboardButton("✅ ЦЕ ВСЕ!", "menu");

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(createKeyboardRow(continueButton));
        rowList.add(createKeyboardRow(endButton));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }
    private InlineKeyboardMarkup createInlineKeyboardMarkupForEdit(){
        InlineKeyboardButton editNameButton = createInlineKeyboardButton("✏️ РЕДАГУВАТИ НАЗВУ", "editName");
        InlineKeyboardButton editTimeButton = createInlineKeyboardButton("✏️ РЕДАГУВАТИ ЧАС ПОЧАТКУ", "editTimeStart");
        InlineKeyboardButton editEndTimeButton = createInlineKeyboardButton("✏️ РЕДАГУВАТИ ЧАС ЗАКІНЧЕННЯ", "editTimeEnd");
        InlineKeyboardButton weekButton = createInlineKeyboardButton("\uD83D\uDDD3\uFE0F ВИБРАТИ ІНШИЙ ДЕНЬ", "backToWeek");
        InlineKeyboardButton menuButton = createInlineKeyboardButton("\uD83D\uDDC4\uFE0F МЕНЮ", "menu");

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(createKeyboardRow(editNameButton));
        rowList.add(createKeyboardRow(editTimeButton));
        rowList.add(createKeyboardRow(editEndTimeButton));
        rowList.add(createKeyboardRow(weekButton));
        rowList.add(createKeyboardRow(menuButton));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }
    private InlineKeyboardMarkup createInlineKeyboardMarkupForEndEdit(){
        InlineKeyboardButton continueEditingButton = createInlineKeyboardButton("➡\uFE0F ПРОДОВЖИТИ", "edit");
        InlineKeyboardButton menuButton = createInlineKeyboardButton("\uD83D\uDDC4\uFE0F МЕНЮ", "menu");

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(createKeyboardRow(continueEditingButton));
        rowList.add(createKeyboardRow(menuButton));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }
    private InlineKeyboardMarkup createInlineKeyboardMarkupForShow(){
        InlineKeyboardButton showWeekButton = createInlineKeyboardButton("\uD83D\uDDD3\uFE0F ВЕСЬ ТИЖДЕНЬ", "showWeek");
        InlineKeyboardButton showDayButton = createInlineKeyboardButton("\uD83D\uDDD3\uFE0F ОБРАТИ ДЕНЬ", "showDay");
        InlineKeyboardButton showNearestPlanButton = createInlineKeyboardButton("\uD83D\uDD70\uFE0F НАЙБЛИЖЧИЙ ПЛАН", "showNearestPlan");
        InlineKeyboardButton menuButton = createInlineKeyboardButton("\uD83D\uDDC4\uFE0F МЕНЮ", "menu");

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(createKeyboardRow(showWeekButton));
        rowList.add(createKeyboardRow(showDayButton));
        rowList.add(createKeyboardRow(showNearestPlanButton));
        rowList.add(createKeyboardRow(menuButton));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }
    private InlineKeyboardMarkup createInlineKeyboardMarkupForRandom(){
        InlineKeyboardButton randomButton = createInlineKeyboardButton("Йти чи не Йти?", "random");

        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        rowList.add(createKeyboardRow(randomButton));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        inlineKeyboardMarkup.setKeyboard(rowList);

        return inlineKeyboardMarkup;
    }

    // These methods are display something
    private void displayPlanForDay(DayHandler dayHandler, SendMessage sendMessage, long userId){
        PlanForDay planForDay = dayHandler.getPlansForDay(userId);
        if(planForDay == null || planForDay.getPlans().isEmpty()){
            sendMessage.setText("На цей день плани відсутні \uD83D\uDE0B");
        }
        else{
            StringBuilder plansText = new StringBuilder();
            for(Plan plan : planForDay.getPlans()){
                plansText.append(" ➖ " + plan.getStartTime()).append((plan.getEndTime() == null) ? " " : " - " + plan.getEndTime()).append(" ").append(plan.getPlanName()).append("\n");
            }
            sendMessage.setText(plansText.toString());
        }
        try {
            execute(sendMessage);
        } catch (Exception e) {
            logger.info("An error occurred", e);
        }
        long chatId = Long.parseLong(sendMessage.getChatId());
    }
    private void displayPlanForWeek(DayHandler dayHandler, SendMessage sendMessage, long userId){
        StringBuilder plansText = new StringBuilder();
        UserState userState = dayHandler.getUserStates().get(userId);
        List<String> daysOfWeek = Arrays.asList("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");
        for (String day : daysOfWeek){
            PlanForDay planForDay = userState.getUserState().get(day);
            if(planForDay != null && !planForDay.getPlans().isEmpty()){
                plansText.append("\uD83D\uDDD3\uFE0F " + getDayOfWeek(day) + "\n");
                for(Plan plan : planForDay.getPlans()){
                    plansText.append(" ➖ " + plan.getStartTime()).append((plan.getEndTime() == null) ? " " : " - " + plan.getEndTime()).append(" ").append(plan.getPlanName()).append("\n");
                }
            }
        }
        if(plansText.length() == 0){
            sendMessage.setText("Плани відсутні \uD83D\uDE0B");
        }
        else{
            sendMessage.setText(plansText.toString());
        }
        try {
            execute(sendMessage);
        } catch (Exception e) {
            logger.info("An error occurred", e);
        }
        long chatId = Long.parseLong(sendMessage.getChatId());
        printMenu(chatId);
    }
    private void displayNearestPlan(DayHandler dayHandler, SendMessage sendMessage, long userId){
        LocalDate localDate = LocalDate.now();
        String day = localDate.getDayOfWeek().name().toLowerCase();
        dayHandler.setDay(day);
        PlanForDay planForDay = dayHandler.getPlansForDay(userId);
        if(planForDay == null || planForDay.getPlans().isEmpty()){
            sendMessage.setText("Плани відсутні \uD83D\uDE0B");
        }
        else{
            LocalTime currentTime = LocalTime.now();
            Plan nearestPlan = planForDay.getPlans().stream().filter(plan -> plan.getEndTime() == null || plan.getEndTime().isAfter(currentTime)).min(Comparator.comparing(Plan::getStartTime)).orElse(null);
            if(nearestPlan == null){
                sendMessage.setText("Плани відсутні \uD83D\uDE0B");
            }
            else{
                sendMessage.setText("Найближчий план: " + nearestPlan.getStartTime() + " - " + (nearestPlan.getEndTime() == null ? " " : nearestPlan.getEndTime()) + " " + nearestPlan.getPlanName());
            }
        }
        try {
            execute(sendMessage);
        } catch (Exception e) {
            logger.info("An error occurred", e);
        }
        long chatId = Long.parseLong(sendMessage.getChatId());
        printMenu(chatId);
    }
    private void printMenu(long chatId){
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
    private String getDayOfWeek(String day){
        switch (day){
            case "monday": return "Понеділок";
            case "tuesday": return "Вівторок";
            case "wednesday": return "Середа";
            case "thursday": return "Четвер";
            case "friday": return "П'ятниця";
            case "saturday": return "Субота";
            case "sunday": return "Неділя";
        }
        return null;
    }

    private void resetTimer(long chatId){
        if (chatTimers.get(chatId) != null){
            chatTimers.get(chatId).cancel();
        }

        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                chatTimers.put(chatId, null);
            }
        }, delay);

        chatTimers.put(chatId, timer);
    }

    public timeManagerBot(){
        DayHandler dayHandlerInf = new DayHandler();
        dayHandlerInf.scheduleReminder(this);
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


    public class ReminderTask extends TimerTask {
        @Override
        public void run() {
            LocalDate nextDay = LocalDate.now().plusDays(1);
            String day = nextDay.getDayOfWeek().name().toLowerCase();

            PlanForDay planForDay = DayHandler.informaticsPlan.get(day);

            String planString = planForDay.getPlans().stream()
                    .map(plan -> " ➖ " + plan.getStartTime() + " - " + plan.getEndTime() + "\n" + plan.getPlanName())
                    .collect(Collectors.joining("\n"));
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(dotenv.get("INFORMATICS_CHAT_ID"));
            sendMessage.setText("⏰ ЗАВТРАШНІ ЛЕКЦІЇ: \n \n" + planString);

            sendMessage.setReplyMarkup(createInlineKeyboardMarkupForRandom());

            try {
                execute(sendMessage);
            } catch (Exception e) {
                logger.info("An error occurred", e);
            }
        }
    }
    public class ReminderBeforePlanStarts extends TimerTask {
        @Override
        public void run() {
            LocalTime currentTime = LocalTime.now();
            PlanForDay planForDay = DayHandler.informaticsPlan.get(LocalDate.now().getDayOfWeek().name().toLowerCase());

            for (Plan plan : planForDay.getPlans()){
                if (plan.getStartTime().minusMinutes(30).equals(currentTime)){
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(dotenv.get("INFORMATICS_CHAT_ID"));
                    sendMessage.setText("Маша прийди, Маша прийди, Маша прийди 🙏");


                    try {
                        execute(sendMessage);
                    } catch (Exception e) {
                        logger.info("An error occurred", e);
                    }
                }
                else if (plan.getStartTime().equals(currentTime)){
                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(dotenv.get("INFORMATICS_CHAT_ID"));
                    sendMessage.setText("Маша ти тут? (Так чи Ні)");

                    try {
                        execute(sendMessage);
                    } catch (Exception e) {
                        logger.info("An error occurred", e);
                    }
                }
            }
        }
    }
}
