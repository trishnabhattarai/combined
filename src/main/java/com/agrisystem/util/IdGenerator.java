package com.agrisystem.util;

import com.agrisystem.service.DataService;

public class IdGenerator {

    public static String nextUserId(String type) {
        String prefix = switch (type.toUpperCase()) {
            case "ADMIN" -> "USR-A-";
            case "OFFICER" -> "USR-O-";
            default -> "USR-F-";
        };
        int max = 0;
        for (var user : DataService.getInstance().getAllUsers()) {
            if (user.getId().startsWith(prefix)) {
                try {
                    int num = Integer.parseInt(user.getId().substring(prefix.length()));
                    if (num > max) max = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%03d", prefix, max + 1);
    }

    public static String nextLoanId() {
        String prefix = "LOA-";
        int max = 0;
        for (var loan : DataService.getInstance().getAllLoans()) {
            if (loan.getId().startsWith(prefix)) {
                try {
                    int num = Integer.parseInt(loan.getId().substring(prefix.length()));
                    if (num > max) max = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%03d", prefix, max + 1);
    }

    public static String nextFieldId() {
        String prefix = "FIE-";
        int max = 0;
        for (var field : DataService.getInstance().getAllFields()) {
            if (field.getId().startsWith(prefix)) {
                try {
                    int num = Integer.parseInt(field.getId().substring(prefix.length()));
                    if (num > max) max = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%03d", prefix, max + 1);
    }

    public static String nextTransactionId() {
        String prefix = "TRX-";
        int max = 0;
        for (var t : DataService.getInstance().getAllTransactions()) {
            if (t.getId().startsWith(prefix)) {
                try {
                    int num = Integer.parseInt(t.getId().substring(prefix.length()));
                    if (num > max) max = num;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%03d", prefix, max + 1);
    }
}
