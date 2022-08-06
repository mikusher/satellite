package com.mikusher.converters;


import com.mikusher.error.IncorrectTypeException;

import java.util.Calendar;
import java.util.Date;

public class ConversionUtils {

    public static final String DATE_LONG_FORMAT = "yyyyMMddHHmmss";

    private static final ThreadLocal<Calendar> _calendar = ThreadLocal.withInitial(Calendar::getInstance);


    private ConversionUtils() {
    }

    public static final boolean isFinite(double value) {

        return value != Double.NaN && value != Double.NEGATIVE_INFINITY && value != Double.POSITIVE_INFINITY;
    }

    public static final boolean isFinite(float value) {

        return value != Float.NaN && value != Float.NEGATIVE_INFINITY && value != Float.POSITIVE_INFINITY;
    }

    public static boolean hasDecimalPart(double value) {

        return value % 1 != 0;
    }

    public static final Calendar getCalendar() {

        return _calendar.get();
    }

    @Deprecated
    public static Date string2Date(CharSequence dateObj) throws IncorrectTypeException {

        return FastDateFormat.SECOND.string2Date(dateObj);
    }

    @Deprecated
    public static Date long2Date(long dateLng) throws IncorrectTypeException {

        return FastDateFormat.SECOND.long2Date(dateLng);
    }

    @Deprecated
    public static long date2Long(Date dateObj) {

        return FastDateFormat.SECOND.date2Long(dateObj);
    }

    @Deprecated
    public static String date2String(Date dateObj) {

        return FastDateFormat.SECOND.date2String(dateObj);
    }


    public enum FastDateFormat {
        DAY("yyyyMMdd", 1_01_01L, 9999_99_99L),
        HOUR("yyyyMMddHH", 1_01_01_00L, 9999_99_99_99L),
        MINUTE("yyyyMMddHHmm", 1_01_01_00_00L, 9999_99_99_99_99L),
        SECOND("yyyyMMddHHmmss", 1_01_01_00_00_00L, 9999_99_99_99_99_99L);

        private final String _format;
        private final long _minValue;
        private final long _maxValue;
        private final long _formatValue;
        private final long _minFormatValue;


        FastDateFormat(String format, long minValue, long maxValue) {

            _format = format;
            _minValue = minValue;
            _maxValue = maxValue;
            _formatValue = maxValue + 1;
            _minFormatValue = maxValue / 10;
        }

        public static FastDateFormat findFormat(String format) {

            for (FastDateFormat elem : FastDateFormat.values()) {
                if (elem._format.equals(format)) {
                    return elem;
                }
            }

            return null;
        }

        public boolean isDate(CharSequence value) {

            try {
                return isDate(Long.parseLong(value.toString()));
            } catch (NumberFormatException e) {
                return false;
            }
        }

        public boolean isDate(long value) {

            return value >= _minValue && value <= _maxValue;
        }

        /**
         * @param dateObj
         * @return
         * @throws IncorrectTypeException
         */
        public Date string2Date(CharSequence dateObj) throws IncorrectTypeException {

            return long2Date(Long.parseLong(dateObj.toString()));
        }

        /**
         * @param dateLng
         * @return
         * @throws IncorrectTypeException
         */
        public Date long2Date(long dateLng) throws IncorrectTypeException {

            if (dateLng < _minValue || dateLng > _maxValue) {
                throw new IncorrectTypeException(Date.class, Long.class, dateLng);
            }

            Calendar cal = getCalendar();
            longToCal(dateLng, cal);
            return cal.getTime();
        }

        /**
         * @param dateObj
         * @return
         * @throws IncorrectTypeException
         */
        public long date2Long(Date dateObj) {

            Calendar cal = getCalendar();
            cal.setTime(dateObj);

            return calToLong(cal);
        }

        /**
         * @param dateObj
         * @return
         */
        public String date2String(Date dateObj) {

            Calendar cal = getCalendar();
            cal.setTime(dateObj);
            long value = calToLong(cal);

            if (value > _minFormatValue) {
                return Long.toString(value);
            }

            return Long.toString(value + _formatValue).substring(1);
        }

        private void longToCal(long value, Calendar cal) {

            // Truncate
            switch (this) {
                case DAY:
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                case HOUR:
                    cal.set(Calendar.MINUTE, 0);
                case MINUTE:
                    cal.set(Calendar.SECOND, 0);
                default:
                    cal.set(Calendar.MILLISECOND, 0);
            }

            switch (this) {
                case SECOND:
                    cal.set(Calendar.SECOND, (int) (value % 100));
                    value /= 100;

                case MINUTE:
                    cal.set(Calendar.MINUTE, (int) (value % 100));
                    value /= 100;

                case HOUR:
                    cal.set(Calendar.HOUR_OF_DAY, (int) (value % 100));
                    value /= 100;

                case DAY:
                    cal.set(Calendar.DAY_OF_MONTH, (int) (value % 100));
                    value /= 100;
                    cal.set(Calendar.MONTH, (int) (value % 100) - 1);
                    value /= 100;
                    cal.set(Calendar.YEAR, (int) value);
            }
        }

        private long calToLong(Calendar cal) {

            long value = 0L;
            long factor = 1L;
            switch (this) {
                case SECOND:
                    value += cal.get(Calendar.SECOND) * factor;
                    factor *= 100L;

                case MINUTE:
                    value += cal.get(Calendar.MINUTE) * factor;
                    factor *= 100L;

                case HOUR:
                    value += cal.get(Calendar.HOUR_OF_DAY) * factor;
                    factor *= 100L;

                case DAY:
                    value += cal.get(Calendar.DAY_OF_MONTH) * factor;
                    factor *= 100L;
                    value += (cal.get(Calendar.MONTH) + 1) * factor;
                    factor *= 100L;
                    value += cal.get(Calendar.YEAR) * factor;
            }

            return value;
        }

        public String getFormat() {

            return _format;
        }

    }

}
