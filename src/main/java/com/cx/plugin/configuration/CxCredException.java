package com.cx.plugin.configuration;

import com.cx.restclient.exception.CxClientException;


/**
 * Created by SubhadraS on 04/02/2021
 */
public class CxCredException extends CxClientException {

    public CxCredException() {
        super();
    }

    public CxCredException(String message) {
        super(message);
    }

    public CxCredException(String message, Throwable cause) {
        super(message, cause);
    }

    public CxCredException(Throwable cause) {
        super(cause);
    }
}
