package com.umcs.lessons;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class Plan implements Comparable<Plan> {
    private String planName;
    private LocalTime startTime;
    private LocalTime endTime;
    @Override
    public int compareTo(Plan o){
        return this.startTime.compareTo(o.startTime);
    }
}
