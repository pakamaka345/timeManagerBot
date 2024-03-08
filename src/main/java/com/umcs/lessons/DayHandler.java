package com.umcs.lessons;



import java.util.HashMap;
import java.util.Map;

public class DayHandler {
    private Map<Long, UserState> userStates = new HashMap<>();
    public void addPlanForDay(Long userId, String day, Plan plan){
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
    public void deletePlanForDay(Long userId, String day, String planName){
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
    public PlanForDay getPlansForDay(Long userId, String day){
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
}
