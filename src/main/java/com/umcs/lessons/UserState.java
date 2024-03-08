package com.umcs.lessons;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
@Getter
public class UserState {
    private long userId;
    private Map<String, PlanForDay> userState = new HashMap<>();
    public UserState(long userId){
        this.userId = userId;
    }
    public void addPlanForDay(String day, PlanForDay planForDay){
        userState.put(day, planForDay);
    }
}
