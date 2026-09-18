package com.ecommerce.cnj70.config;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * Format a sold count using Shopee-style short notation:
     *   0   -> "0"
     *   120 -> "120"
     *   999 -> "999"
     *   1.2k, 9.9k   for values 1,000–9,999
     *   10k+, 999k+   for values >= 10,000
     */
    public String formatSold(Object value) {
        long n;
        if (value == null) {
            return "0";
        }
        if (value instanceof Number num) {
            n = num.longValue();
        } else {
            try {
                n = Long.parseLong(value.toString());
            } catch (NumberFormatException ex) {
                return "0";
            }
        }
        if (n <= 0) return "0";
        if (n < 1000) return String.valueOf(n);
        if (n < 10_000) {
            double k = n / 1000.0;
            return (Math.round(k * 10.0) / 10.0) + "k";
        }
        return (n / 1000) + "k+";
    }

    /**
     * Stable discount percentage derived from a product ID hash.
     * Returns a value between 20% and 60% so the same product always shows
     * the same discount across page reloads.
     */
    public int flashDiscount(Object id) {
        int hash = Math.abs(String.valueOf(id).hashCode());
        return 20 + (hash % 41); // 20..60
    }

    /**
     * Stable flash sale progress percentage (20%..90%) derived from the
     * product ID, so the bar fills differently per product but consistently.
     */
    public int flashProgress(Object id) {
        int hash = Math.abs(String.valueOf(id).hashCode());
        return 20 + (hash % 71); // 20..90
    }

    /**
     * Original (pre-discount) price for a flash sale product, computed by
     * marking up the current price with the stable {@link #flashDiscount}.
     * Same input always yields the same output.
     */
    public String flashOriginalPrice(Object id, Object currentPrice) {
        BigDecimal current;
        if (currentPrice instanceof BigDecimal bd) {
            current = bd;
        } else if (currentPrice instanceof Number n) {
            current = BigDecimal.valueOf(n.doubleValue());
        } else {
            return format(null);
        }
        int pct = flashDiscount(id);
        BigDecimal original = current.multiply(BigDecimal.valueOf(100 + pct))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return VND.format(original) + " ₫";
    }

    /**
     * Convenience overload for Thymeleaf: format a BigDecimal directly.
     */
    public String format(BigDecimal value) {
        return format((Object) value);
    }

    /**
     * Format cartTotal + shipping fee (15,000 VND) as the order total.
     * Used by the checkout page where SpEL cannot reference java.math.BigDecimal
     * directly inside Thymeleaf expressions.
     */
    public String formatTotalWithShipping(Object cartTotal) {
        BigDecimal cart;
        if (cartTotal instanceof BigDecimal bd) {
            cart = bd;
        } else if (cartTotal instanceof Number n) {
            cart = BigDecimal.valueOf(n.doubleValue());
        } else {
            cart = BigDecimal.ZERO;
        }
        BigDecimal shipping = BigDecimal.valueOf(15000);
        return format(cart.add(shipping));
    }
}
