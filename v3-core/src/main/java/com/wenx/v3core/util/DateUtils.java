package com.wenx.v3core.util;

import com.alibaba.fastjson2.JSON;
import org.apache.commons.lang3.time.DateFormatUtils;

import java.lang.management.ManagementFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <ul>时间工具类 <ul/>
 * <li>参考ruo-yi 需要补充需要的日期格式 <li/>
 *
 * @author wenx
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils {
    public static String YYYY = "yyyy";
    public static String MM = "MM";

    public static String YYYY_MM = "yyyy-MM";

    public static String YYYYMM = "yyyyMM";

    public static String YYYYMMDD = "yyyyMMdd";

    public static String HHMM = "HHmm";

    public static String HH_MM = "HH:mm";

    public static String HH_MM_SS = "HH:mm:ss";

    public static String YYYY_MM_DD = "yyyy-MM-dd";

    public static String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";

    public static String YYYY_MM_DD_HH_MM_SS = "yyyy-MM-dd HH:mm:ss";

    private static String[] parsePatterns = {
            "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM",
            "yyyy/MM/dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd HH:mm", "yyyy/MM",
            "yyyy.MM.dd", "yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd HH:mm", "yyyy.MM"};

    /**
     * 获取当前Date型日期
     *
     * @return Date() 当前日期
     */
    public static Date getNowDate() {
        return dateTime(YYYY_MM_DD_HH_MM_SS, getTime());
    }

    /**
     * 获取当前日期, 默认格式为yyyy-MM-dd
     *
     * @return String
     */
    public static String getDate() {
        return dateTimeNow(YYYY_MM_DD);
    }

    public static String getTime() {
        return dateTimeNow(YYYY_MM_DD_HH_MM_SS);
    }

    public static String dateTimeNow() {
        return dateTimeNow(YYYYMMDDHHMMSS);
    }

    public static String dateTimeNow(final String format) {
        return parseDateToStr(format, new Date());
    }

    public static String dateTime(final Date date) {
        return parseDateToStr(YYYY_MM_DD, date);
    }

    public static String dateHHmm(final Date date) {
        return parseDateToStr(HH_MM, date);
    }

    public static String dateYYYY(final Date date) {
        return parseDateToStr(YYYY, date);
    }

    public static String dateMM(final Date date) {
        return parseDateToStr(MM, date);
    }

    public static String parseDateToStr(final String format, final Date date) {
        return new SimpleDateFormat(format).format(date);
    }

    public static Date dateTime(final String format, final String ts) {
        try {
            return new SimpleDateFormat(format).parse(ts);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 日期路径 即年/月/日 如2018/08/08
     */
    public static String datePath() {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyy/MM/dd");
    }

    /**
     * 日期路径 即年/月/日 如20180808
     */
    public static String dateTime() {
        Date now = new Date();
        return DateFormatUtils.format(now, "yyyyMMdd");
    }

    /**
     * 日期型字符串转化为日期 格式
     */
    public static Date parseDate(Object str) {
        if (str == null) {
            return null;
        }
        try {
            return parseDate(str.toString(), parsePatterns);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 获取服务器启动时间
     */
    public static Date getServerStartDate() {
        long time = ManagementFactory.getRuntimeMXBean().getStartTime();
        return new Date(time);
    }

    /**
     * 计算两个时间差
     */
    public static String getDatePoor(Date endDate, Date nowDate) {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        // 计算差多少小时
        long hour = diff % nd / nh;
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        // 计算差多少秒//输出结果
        // long sec = diff % nd % nh % nm / ns;
        return day + "天" + hour + "小时" + min + "分钟";
    }

    public static Double getHoursPoor(Date endDate, Date nowDate) {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少天
        // 计算差多少小时
        long hour = diff % nd / nh;
        return (double) hour;
    }

    public static Double getSummaryDatePoor(Date endDate, Date nowDate) {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        // long ns = 1000;
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少小时
        long hour = diff % nd / nh;
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        if (min < 30) {
            return (double) (hour);
        } else if (min == 30) {
            return hour + 0.5;
        } else if (min > 30) {
            return (double) (hour + 1);
        }
        return (double) (hour);
    }


    /**
     * 根据年月算出当前月的每一天
     *
     * @param yearParam
     * @param monthParam
     * @return
     */
    public static List<String> getDayByMonth(int yearParam, int monthParam) {
        List<String> result = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat(YYYY_MM_DD);
        Date _date = null;

        try {
            _date = sdf.parse(yearParam + "-" + String.format("%02d", monthParam) + "-" + "01");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(_date);
            int actualMaximum = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int i = 1; i <= actualMaximum; i++) {
                    result.add(yearParam + "/" + String.format("%02d", monthParam) + "/" + String.format("%02d", i));
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("无效的年月参数: " + yearParam + "-" + monthParam, e);

        }
        return result;
    }

    /**
     * 判断日期格式和范围
     */
    public static boolean isDate(String date) {
        String rexp = "^((\\d{2}(([02468][048])|([13579][26]))[\\-\\/\\s]?((((0?[13578])|(1[02]))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(3[01])))|(((0?[469])|(11))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(30)))|(0?2[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])))))|(\\d{2}(([02468][1235679])|([13579][01345789]))[\\-\\/\\s]?((((0?[13578])|(1[02]))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(3[01])))|(((0?[469])|(11))[\\-\\/\\s]?((0?[1-9])|([1-2][0-9])|(30)))|(0?2[\\-\\/\\s]?((0?[1-9])|(1[0-9])|(2[0-8]))))))";
        Pattern pat = Pattern.compile(rexp);
        Matcher mat = pat.matcher(date);
        boolean dateType = mat.matches();
        return dateType;
    }

    public static List<Long> day2Hours_max(Date nowDate, Date endDate) {
        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;
        long ns = 1000;
        List<Long> dateList = new ArrayList<>();
        // 获得两个时间的毫秒时间差异
        long diff = endDate.getTime() - nowDate.getTime();
        // 计算差多少天
        long day = diff / nd;
        dateList.add(day);
        // 计算差多少小时
        long hour = diff % nd / nh;
        dateList.add(hour);
        // 计算差多少分钟
        long min = diff % nd % nh / nm;
        dateList.add(min);
        long sec = diff % nd % nh % nm / ns;
        dateList.add(sec);
        return dateList;
    }

    public static void main(String[] args) {

        getDayByMonth(2021, 10);

        String datestr = "2021-01-02 09:00:00";
        String datestrss = "2021-01-04 19:30:00";
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTime(dateTime(YYYY_MM_DD_HH_MM_SS, datestr));
//        System.out.println(getSummaryDatePoor(dateTime(YYYY_MM_DD_HH_MM_SS, datestrss), dateTime(YYYY_MM_DD_HH_MM_SS, datestr)));
        int year = rightNow.get(Calendar.YEAR);
        int month = rightNow.get(Calendar.MONTH) + 1;
        int day = rightNow.get(Calendar.DAY_OF_MONTH);
        List yearList = new ArrayList();
        yearList.add(year);
        List monthList = new ArrayList();
        monthList.add(month);
        if (day <= 6) {
            if (month > 1) {
                monthList.add(month - 1);
            } else {
                yearList.add(year - 1);
                monthList.add(12);
            }
        }
        System.out.println(JSON.toJSONString(yearList));
        System.out.println(JSON.toJSONString(monthList));
        yearList.stream().forEach(years -> {
            monthList.stream().forEach(months -> {
                Date summaryDate = DateUtils.dateTime(DateUtils.YYYY_MM_DD, years + "-" + months + "-01");
                long nd = 1000 * 24 * 60 * 60;
                // long ns = 1000;
                // 获得两个时间的毫秒时间差异
                String datab = "2021-01-04";
                long diff = dateTime(YYYY_MM_DD, datab).getTime() - summaryDate.getTime();
                // 计算差多少天
                long days = diff / nd;
                System.out.println(years + "-" + months);
                System.out.println(days);
                if (days < 60 && days > -6) {
                    System.out.println("---" + years + "-" + months);
                }
            });
        });
    }
}
