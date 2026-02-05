package com.passmanager.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class PasswordGeneratorUtil {

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String AMBIGUOUS = "0O1lI";

    private final SecureRandom random = new SecureRandom();

    public String generate(int length, boolean includeLowercase, boolean includeUppercase,
                          boolean includeDigits, boolean includeSymbols, boolean excludeAmbiguous) {

        if (length < 4) {
            length = 4;
        }
        if (length > 128) {
            length = 128;
        }

        StringBuilder charPool = new StringBuilder();
        StringBuilder password = new StringBuilder();

        // Construir el pool de caracteres
        if (includeLowercase) {
            String lower = excludeAmbiguous ? removeAmbiguous(LOWERCASE) : LOWERCASE;
            charPool.append(lower);
            // Garantizar al menos un caracter de este tipo
            password.append(lower.charAt(random.nextInt(lower.length())));
        }
        if (includeUppercase) {
            String upper = excludeAmbiguous ? removeAmbiguous(UPPERCASE) : UPPERCASE;
            charPool.append(upper);
            password.append(upper.charAt(random.nextInt(upper.length())));
        }
        if (includeDigits) {
            String digits = excludeAmbiguous ? removeAmbiguous(DIGITS) : DIGITS;
            charPool.append(digits);
            password.append(digits.charAt(random.nextInt(digits.length())));
        }
        if (includeSymbols) {
            charPool.append(SYMBOLS);
            password.append(SYMBOLS.charAt(random.nextInt(SYMBOLS.length())));
        }

        // Si no se seleccionó ninguna opción, usar minúsculas por defecto
        if (charPool.length() == 0) {
            charPool.append(LOWERCASE);
            password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        }

        // Completar la contraseña hasta la longitud deseada
        String pool = charPool.toString();
        while (password.length() < length) {
            password.append(pool.charAt(random.nextInt(pool.length())));
        }

        // Mezclar los caracteres para que los garantizados no estén siempre al inicio
        return shuffle(password.toString());
    }

    private String removeAmbiguous(String str) {
        StringBuilder result = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (AMBIGUOUS.indexOf(c) == -1) {
                result.append(c);
            }
        }
        return result.toString();
    }

    private String shuffle(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    public PasswordStrength evaluateStrength(String password) {
        if (password == null || password.isEmpty()) {
            return PasswordStrength.VERY_WEAK;
        }

        int score = 0;
        int length = password.length();

        // Puntuación por longitud
        if (length >= 8) score++;
        if (length >= 12) score++;
        if (length >= 16) score++;
        if (length >= 20) score++;

        // Puntuación por tipos de caracteres
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[0-9].*")) score++;
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?].*")) score++;

        // Penalización por patrones comunes
        if (password.matches(".*(.)\\1{2,}.*")) score--; // Caracteres repetidos
        if (password.matches(".*(?:012|123|234|345|456|567|678|789|890).*")) score--; // Secuencias numéricas
        if (password.matches(".*(?:abc|bcd|cde|def|efg|fgh|ghi|hij|ijk|jkl|klm|lmn|mno|nop|opq|pqr|qrs|rst|stu|tuv|uvw|vwx|wxy|xyz).*")) score--; // Secuencias alfabéticas

        if (score <= 2) return PasswordStrength.VERY_WEAK;
        if (score <= 4) return PasswordStrength.WEAK;
        if (score <= 6) return PasswordStrength.MEDIUM;
        if (score <= 8) return PasswordStrength.STRONG;
        return PasswordStrength.VERY_STRONG;
    }

    public enum PasswordStrength {
        VERY_WEAK("Muy débil", 0.2, "#ef4444"),
        WEAK("Débil", 0.4, "#f97316"),
        MEDIUM("Media", 0.6, "#eab308"),
        STRONG("Fuerte", 0.8, "#22c55e"),
        VERY_STRONG("Muy fuerte", 1.0, "#15803d");

        private final String label;
        private final double progress;
        private final String color;

        PasswordStrength(String label, double progress, String color) {
            this.label = label;
            this.progress = progress;
            this.color = color;
        }

        public String getLabel() { return label; }
        public double getProgress() { return progress; }
        public String getColor() { return color; }
    }
}
