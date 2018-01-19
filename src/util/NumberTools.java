/*
 * Copyright (c) 2013, Aleksey Nikolaevich Dokshin. All right reserved.
 * Contacts: dant.it@gmail.com, dokshin@list.ru.
 */
package util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Расширенный функционал для операций с числами.
 *
 * @author Докшин Алексей Николаевич <dant.it@gmail.com>
 */
public class NumberTools {

    /**
     * Предопределенный формат для вывода с точностью 0 знаков после запятой.
     */
    public static NumberFormat format0 = new DecimalFormat("#,##0");
    /**
     * Предопределенный формат для вывода с точностью 1 знак после запятой.
     */
    public static NumberFormat format1 = new DecimalFormat("#,##0.0");
    /**
     * Предопределенный формат для вывода с точностью 2 знака после запятой.
     */
    public static NumberFormat format2 = new DecimalFormat("#,##0.00");
    /**
     * Предопределенный формат для вывода с точностью 3 знака после запятой.
     */
    public static NumberFormat format3 = new DecimalFormat("#,##0.000");

    /**
     * Возвращает форматер для заданного кол-ва знаков после зяпятой.
     *
     * @param digits Кол-во знаков после запятой (0-3).
     * @return Форматер.
     */
    public static NumberFormat getFormat(int digits) {
        switch (digits) {
            case 0:
                return format0;
            case 1:
                return format1;
            case 2:
                return format2;
            case 3:
                return format3;
        }
        return format0;
    }

    /**
     * Преобразует целое число в строку с форматированием.
     *
     * @param value Число.
     * @param f     Форматер для преобразования в строку.
     * @param sNull Строка выводимая при пустом значении.
     * @param sZero Строка выводимая при нулевом значении.
     * @return Форматированная строка.
     */
    public static String integerToFormatString(Integer value, NumberFormat f,
            String sNull, String sZero) {
        if (value == null) {
            return sNull;
        }
        if (value == 0.0) {
            return sZero;
        }
        return f.format(value);
    }

    /**
     * Преобразует число с плавающей точкой в строку с форматированием.
     *
     * @param value Число с плавающей точкой.
     * @param f     Форматер для преобразования в строку.
     * @param sNull Строка выводимая при пустом значении.
     * @param sZero Строка выводимая при нулевом значении.
     * @return Форматированная строка.
     */
    public static String doubleToFormatString(Double value, NumberFormat f,
            String sNull, String sZero) {
        if (value == null) {
            return sNull;
        }
        if (value == 0.0 && sZero != null) {
            return sZero;
        }
        return f.format(value);
    }

    /**
     * Безопасное преобразование строки в Integer.
     *
     * @param src Строка-число.
     * @return Число; null - строка не задана, пустая или имеет неверный формат.
     */
    public static Integer parseInt(String src) {
        if (src == null) {
            return null;
        }
        try {
            return Integer.parseInt(src);
        } catch (Exception ex) {
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Создание объекта Integer из разных видов объектов.
     *
     * @param value Объект-значение.
     * @return Число или null.
     */
    public static Integer toIntegerSafe(final Object value) {
        if (value != null) {
            if (value instanceof Integer) {
                return (Integer) value;
            }
            if (value instanceof BigDecimal) {
                return ((BigDecimal) value).intValue();
            }
            if (value instanceof Long) {
                return ((Long) value).intValue();
            }
            if (value instanceof Double) {
                return ((Double) value).intValue();
            }
        }
        return null;
    }

    /**
     * Создание объекта BigDecimal из разных видов объектов.
     *
     * @param value Объект-значение.
     * @return Число или null.
     */
    public static BigDecimal toBigDecimalSafe(final Object value) {
        if (value != null) {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof Integer) {
                return new BigDecimal((Integer) value);
            }
            if (value instanceof Long) {
                return new BigDecimal((Long) value);
            }
            if (value instanceof Double) {
                return new BigDecimal((Double) value);
            }
        }
        return null;
    }

    public static boolean isNegativeSafe(final BigDecimal value) {
        return value == null ? false : value.signum() < 0;
    }

    public static boolean isPositiveSafe(final BigDecimal value) {
        return value == null ? false : value.signum() > 0;
    }

    public static boolean isZeroSafe(final BigDecimal value) {
        return value == null ? false : value.signum() == 0;
    }

    /**
     * Сравнение всех значений на равенство нулю. ВНИМАНИЕ! BigDecimal.ZERO.equals() не даёт
     * корректного результата т.к. возвращает false при несовпадении scale даже если значения равны
     * нулю!
     *
     * @param values Список значений.
     * @return true - все равны нулю, false - не все равны нулю.
     */
    public static boolean isAllZero(BigDecimal... values) {
        for (BigDecimal v : values) {
            if (!isZeroSafe(v)) {
                return false;
            }
        }
        return true;
    }
}
