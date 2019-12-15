/*
 * Copyright 2018 Dmitriy Ponomarenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.playerandrecorder.Utills;

import android.content.Context;


import com.example.playerandrecorder.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeUtils {


    private static SimpleDateFormat dateTimeFormat12H = new SimpleDateFormat("MMM dd, hh:mm aa", Locale.US);


    private static SimpleDateFormat dateTimeFormat24H = new SimpleDateFormat("MMM dd, HH:mm", Locale.US);

    private static SimpleDateFormat dateFormat24H = new SimpleDateFormat("MMM dd", Locale.getDefault());

    private static SimpleDateFormat dateTimeFormatEU = new SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault());

    private static SimpleDateFormat dateTimeFormatUS = new SimpleDateFormat("MM/dd/yyyy, HH:mm", Locale.US);

    private static SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy.MM.dd HH.mm.ss", Locale.getDefault());

    private static SimpleDateFormat timeFormatEU = new SimpleDateFormat("HH:mm", Locale.FRANCE);

    private static SimpleDateFormat dateFormatEU = new SimpleDateFormat("dd.MM.yyyy", Locale.FRANCE);

    private TimeUtils() {
    }

    public static String formatTimeIntervalMinSec(long length) {
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;

        long numMinutes = timeUnit.toMinutes(length);
        long numSeconds = timeUnit.toSeconds(length);
        return String.format(Locale.getDefault(), "%02d:%02d", numMinutes, numSeconds % 60);

    }

    public static String formatTimeIntervalHourMinSec2(long length) {
        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        long numHour = timeUnit.toHours(length);
        long numMinutes = timeUnit.toMinutes(length);
        long numSeconds = timeUnit.toSeconds(length);
        if (numHour == 0) {
            return String.format(Locale.getDefault(), "%02d:%02d", numMinutes, numSeconds % 60);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", numHour, numMinutes % 60, numSeconds % 60);
        }
    }

    public static String formatTimeIntervalMinSecMills(long mills) {
        long min = mills / (60 * 1000);
        long sec = (mills / 1000) % 60;
        long m = (mills / 10) % 100;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", min, sec, m);
    }

    public static String formatTimeIntervalHourMinSec(long mills) {
        long hour = mills / (60 * 60 * 1000);
        long min = mills / (60 * 1000) % 60;
        long sec = (mills / 1000) % 60;
        if (hour == 0) {
            if (min == 0) {
                return String.format(Locale.getDefault(), "%02ds", sec);
            } else {
                return String.format(Locale.getDefault(), "%02dm:%02ds", min, sec);
            }
        } else {
            return String.format(Locale.getDefault(), "%02dh:%02dm:%02ds", hour, min, sec);
        }
    }

    public static String formatDateSmart(long time, Context ctx) {
        if (time <= 0) {
            return "Wrong date!";
        }
        Calendar today = Calendar.getInstance();
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(time);
        if (isSameYear(today, date)) {
            if (isSameDay(today, date)) {
                return ctx.getResources().getString(R.string.today);
            } else {
                today.add(Calendar.DAY_OF_YEAR, -1); //Make yesterday
                //Check is yesterday
                if (isSameDay(today, date)) {
                    return ctx.getResources().getString(R.string.yesterday);
                } else {
                    return dateFormat24H.format(new Date(time));
//					return dateFormatEU.format(new Date(time));
                }
            }
        } else {
            return dateFormatEU.format(new Date(time));
        }
    }

    public static String formatTime(long timeMills) {
        if (timeMills <= 0) {
            return "";
        }
        return timeFormatEU.format(new Date(timeMills));
    }


    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }


    public static boolean isSameYear(Calendar cal1, Calendar cal2) {
        if (cal1 == null || cal2 == null) {
            throw new IllegalArgumentException("The dates must not be null");
        }
        return (cal1.get(Calendar.ERA) == cal2.get(Calendar.ERA) &&
                cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR));
    }

    public static String formatDateForName(long time) {
        return dateTimeFormat.format(new Date(time));
    }
}
