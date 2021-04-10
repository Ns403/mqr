package com.molicloud.mqr.plugin.core.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author Ns
 */
public class DateUtil {
    /**
     * 转换时间
     */
    public static LocalDate parseLocalDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
    /**
     * 转换时间
     */
    public static String formatLocalDate(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * 当前时间stirng
     * @return yyyy-MM-dd
     */
    public static String nowFormatLocalDate(){
        return formatLocalDate(LocalDate.now());
    }

}
