package com.umcs.lessons;

import lombok.Getter;
import lombok.Setter;


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
}
