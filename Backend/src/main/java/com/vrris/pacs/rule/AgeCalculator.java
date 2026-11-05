package com.vrris.pacs.rule;

import com.vrris.pacs.enu.DobPrecision;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Tinh tuoi tu ngay sinh (README 2.5: khong luu cot tuoi, tranh loi "Tuoi 0").
 *
 * Quy tac theo doc/02-erd.md muc 4.2 (da chot): khi dob_precision = YEAR, date_of_birth duoc luu
 * la YYYY-01-01 va tuoi hien thi la XAP XI (vi khong biet da qua sinh nhat hay chua), API tra them
 * co "age_approximate".
 *
 * SUY LUAN (02-erd.md muc 4.2 chi noi ro truong hop YEAR): ap dung CUNG quy tac xap xi cho
 * dob_precision = MONTH, vi ngay trong thang van khong biet - cung ly do khong chac da qua "sinh
 * nhat trong thang" hay chua. Chua co nguoi xac nhan rieng cho truong hop MONTH.
 */
public final class AgeCalculator {

    private AgeCalculator() {
    }

    /** approximate = true khi dob_precision khac DAY (xem Javadoc lop nay). */
    public record Result(int years, boolean approximate) {

        public String format() {
            return approximate ? "~" + years : String.valueOf(years);
        }
    }

    public static Result calculate(LocalDate dateOfBirth, DobPrecision precision, LocalDate asOf) {
        Objects.requireNonNull(dateOfBirth, "dateOfBirth");
        Objects.requireNonNull(precision, "precision");
        Objects.requireNonNull(asOf, "asOf");
        if (asOf.isBefore(dateOfBirth)) {
            throw new IllegalArgumentException("asOf khong duoc truoc dateOfBirth");
        }
        int years = Period.between(dateOfBirth, asOf).getYears();
        boolean approximate = precision != DobPrecision.DAY;
        return new Result(years, approximate);
    }
}
