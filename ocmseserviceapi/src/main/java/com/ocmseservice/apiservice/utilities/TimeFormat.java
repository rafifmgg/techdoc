package com.ocmseservice.apiservice.utilities;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeFormat {

    private final LocalDateTime noticeDateTime;

    public TimeFormat(LocalDateTime noticeDateTime) {
        this.noticeDateTime = noticeDateTime;
    }

    public String getFormattedTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmss");
        return noticeDateTime.format(formatter);
    }

    public String getFormattedDate() {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("ddMMyyyy");
        return noticeDateTime.format(dateFormatter);
    }

    @Override
    public String toString() {
        return getFormattedTime();
    }

}