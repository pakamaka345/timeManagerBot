package com.umcs.lessons;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class Plan implements Comparable<Plan> {
    private String id;
    private String planName;
    private LocalTime startTime;
    private LocalTime endTime;
    Plan (){}

    Plan (String id, String planName, LocalTime startTime, LocalTime endTime){
        this.id = id;
        this.planName = planName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    @Override
    public int compareTo(Plan o){
        return this.startTime.compareTo(o.startTime);
    }
}
