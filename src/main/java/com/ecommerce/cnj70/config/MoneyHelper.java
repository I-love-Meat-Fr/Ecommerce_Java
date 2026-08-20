package com.ecommerce.cnj70.config;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Component("money")
public class MoneyHelper {

    private static final NumberFormat VND = NumberFormat.getInstance(new Locale("vi", "VN"));

    public String format(Object value) {
        if (value == null) return "0 ₫";
        BigDecimal amount;
        if (value instanceof BigDecimal bd) {
            amount = bd;
        } else if (value instanceof Number n) {
            amount = BigDecimal.valueOf(n.doubleValue());
        } else {
            return "0 ₫";
        }
        return VND.format(amount) + " ₫";
    }
}
