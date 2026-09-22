package io.github.smaykell.aulavirtual.settings.exception;

import io.github.smaykell.aulavirtual.common.exception.ApiException;

public class ManualIdentifierException extends ApiException {

    public ManualIdentifierException() {
        super(SettingsError.MANUAL_IDENTIFIER);
    }
}
