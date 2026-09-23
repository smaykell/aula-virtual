package io.github.smaykell.aulavirtual.user.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class SuperAdminExclusiveException extends ApiException {

    public SuperAdminExclusiveException() {
        super(UserError.SUPER_ADMIN_EXCLUSIVE);
    }
}
