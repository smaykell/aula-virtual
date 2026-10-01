package io.github.smaykell.aulavirtual.exam.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class BankRequiresStaffException extends ApiException {

    public BankRequiresStaffException() {
        super(ExamError.BANK_REQUIRES_STAFF);
    }
}
