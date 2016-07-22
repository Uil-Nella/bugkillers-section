/*
 * Copyright (c) 2015. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.bugkillers.section;

import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Section 工具类（内部使用）
 * Created by liuxinyu on 15/11/19.
 */
class SectionUtil {

    /**
     * 数字日期格式
     */
    public static final String NUM_DATE_PATTERN = "yyyyMMdd";

    private static final DateTimeFormatter JODA_FORMAT = DateTimeFormat.forPattern(NUM_DATE_PATTERN);

//    /**
//     * 根据起始结束时间，获取每个时间点
//     *
//     * @param startDate
//     * @param endDate
//     * @return
//     */
//    public static List<Integer> getDatePoint(int startDate, int endDate) {
//        List<Integer> list = new ArrayList<Integer>();
//        DateTimeFormatter formatter = DateTimeFormat.forPattern(NUM_DATE_PATTERN);
//        DateTime start = formatter.parseDateTime(String.valueOf(startDate));
//        DateTime end = formatter.parseDateTime(String.valueOf(endDate));
//
//        if (end.isBefore(start)) {
//            return list;
//        }
//
//        while (start.isBefore(end)) {
//            list.add(Integer.parseInt(start.toString(formatter)));
//            start = start.plusDays(1);
//        }
//        list.add(endDate);
//        return list;
//    }

    public static boolean isDateContinuous(int startDate, int endDate) {
        DateTime date1 = new DateTime(startDate / 10000, (startDate % 10000) / 100, (startDate % 10000) % 100, 0, 0);
        DateTime date2 = new DateTime(endDate / 10000, (endDate % 10000) / 100, (endDate % 10000) % 100, 0, 0);
        return Days.daysBetween(date1, date2).getDays() == 1;
    }

    /**
     * 日期减操作
     *
     * @param date
     * @param days
     * @return
     */
    public static int decreaseByDays(int date, int days) {
        DateTime dateTime = stringDateParseDateTime(String.valueOf(date));
        return decreaseByDays(dateTime, days);
    }

    /**
     * 日期格式转换
     *
     * @param strDate
     * @return
     */
    public static DateTime stringDateParseDateTime(String strDate) {
        return DateTime.parse(strDate, JODA_FORMAT);
    }

    /**
     * 日期减操作
     *
     * @param date
     * @param days
     * @return
     */
    public static int decreaseByDays(DateTime date, int days) {
        DateTime dateTime = date.minusDays(days);
        return Integer.parseInt(dateTime.toString(JODA_FORMAT));
    }

    /**
     * 判断日期是否符合格式
     *
     * @param date
     * @return
     */
    public static boolean isDate(int date) {

        DateTime dateTime = null;
        try {
            dateTime = stringDateParseDateTime(String.valueOf(date));
        } catch (Exception e) {
            return false;
        }
        if (dateTime != null) {
            return true;
        }
        return false;
    }

}
