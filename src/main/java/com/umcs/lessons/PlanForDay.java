package com.umcs.lessons;

import lombok.Getter;
import lombok.Setter;


import java.time.LocalTime;
import java.util.TreeSet;

@Getter
@Setter
public class PlanForDay {
    private TreeSet<Plan> plans = new TreeSet<>();

    public void addPlan(Plan plan){
        plans.add(plan);
    }
    public TreeSet<Plan> getPlans(){
        return plans;
    }
    public void deletePlan(String planName){
        plans.removeIf(plan -> plan.getPlanName().equals(planName));
    }
    public void editName(String newName, Plan plan){
        plans.stream().filter(p -> p.equals(plan)).forEach(p -> p.setPlanName(newName));
    }
    public void editStartTime(LocalTime newStartTime, Plan plan){
        plans.stream().filter(p -> p.equals(plan)).forEach(p -> p.setStartTime(newStartTime));
    }
    public void editEndTime(LocalTime newEndTime, Plan plan){
        plans.stream().filter(p -> p.equals(plan)).forEach(p -> p.setEndTime(newEndTime));
    }
}
