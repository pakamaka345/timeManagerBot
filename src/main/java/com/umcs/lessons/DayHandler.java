package com.umcs.lessons;



import com.umcs.lessons.actions.IsAction;
import com.umcs.lessons.actions.IsStart;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

@Getter
@Setter
public class DayHandler {
    private Map<Long, UserState> userStates = new HashMap<>();
    private IsAction isAction;
    private IsStart isStart;
    private Plan plan;
    private String day;

    public void addPlanForDay(Long userId, Plan plan){
        UserState userState = userStates.get(userId);
        if(userState == null){
            userState = new UserState(userId);
            userStates.put(userId, userState);
        }
        PlanForDay planForDay = userState.getUserState().get(day);
        if(planForDay == null){
            planForDay = new PlanForDay();
            userState.addPlanForDay(day, planForDay);
        }
        planForDay.addPlan(plan);
    }
    public void deletePlanForDay(Long userId, String planName){
        UserState userState = userStates.get(userId);
        if(userState == null){
            return;
        }
        PlanForDay planForDay = userState.getUserState().get(day);
        if(planForDay == null){
            return;
        }
        planForDay.deletePlan(planName);
    }
    public PlanForDay getPlansForDay(Long userId){
        UserState userState = userStates.get(userId);
        if(userState == null){
            return null;
        }
        PlanForDay planForDay = userState.getUserState().get(day);
        if(planForDay == null){
            return null;
        }
        return planForDay;
    }



    public static final Map<String, PlanForDay> informaticsPlan = new HashMap<>();

    static {
        PlanForDay mondayPlan = new PlanForDay();

        mondayPlan.addPlan(new Plan("1", "Systemy operacyjne", LocalTime.of(11,30), LocalTime.of(13, 15)));
        mondayPlan.addPlan(new Plan("2", "Analiza matematyczna", LocalTime.of(13,15), LocalTime.of(15, 00)));
        informaticsPlan.put("monday", mondayPlan);

        PlanForDay tuesdayPlan = new PlanForDay();
        tuesdayPlan.addPlan(new Plan("1", "Matematyka dyskretna", LocalTime.of(8,00), LocalTime.of(9, 45)));
        tuesdayPlan.addPlan(new Plan("2", "Bazy danych i zarządzanie informacją", LocalTime.of(10,00), LocalTime.of(11, 45)));
        tuesdayPlan.addPlan(new Plan("3", "Architektury systemów komputerowych", LocalTime.of(11,45), LocalTime.of(13, 15)));
        informaticsPlan.put("tuesday", tuesdayPlan);

        PlanForDay wednesdayPlan = new PlanForDay();
        wednesdayPlan.addPlan(new Plan("1", "Programowanie obiektowe", LocalTime.of(8,00), LocalTime.of(9, 45)));
        informaticsPlan.put("wednesday", wednesdayPlan);

        PlanForDay thursdayPlan = new PlanForDay();
        thursdayPlan.addPlan(new Plan("1", "Zagadnienia prawne ochrony włas. int.", LocalTime.of(8,15), LocalTime.of(10, 00)));
    }

    public void scheduleReminder(timeManagerBot bot){
        Timer timer = new Timer();
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 21);
        date.set(Calendar.MINUTE, 40);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 1);
        timer.schedule(bot.new ReminderTask(), date.getTime(), 1000*60*60*24);
    }
}
