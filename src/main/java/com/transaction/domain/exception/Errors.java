package com.transaction.domain.exception;

public interface Errors {

    interface CreateTransaction {
        String errorCode = "05";

        Error INVALID_INPUT = new Error(errorCode + "01");
        Error NOT_FOUND = new Error(errorCode + "02");
        Error PERSISTENCE_ERROR = new Error(errorCode + "03");
    }

    interface DeleteTransaction {
        String errorCode = "06";

        Error INVALID_INPUT = new Error(errorCode + "01");
        Error NOT_FOUND = new Error(errorCode + "02");
        Error PERSISTENCE_ERROR = new Error(errorCode + "03");
    }

    interface UpdateTransaction {
        String errorCode = "07";

        Error INVALID_INPUT = new Error(errorCode + "01");
        Error NOT_FOUND = new Error(errorCode + "02");
        Error PERSISTENCE_ERROR = new Error(errorCode + "03");
    }

    interface GetTransaction {
        String errorCode = "08";

        Error INVALID_INPUT = new Error(errorCode + "01");
        Error NOT_FOUND = new Error(errorCode + "02");
        Error PERSISTENCE_ERROR = new Error(errorCode + "03");
    }

    interface PublishTransaction {
        String errorCode = "09";

        Error PUBLISH_ERROR = new Error(errorCode + "01");
    }
}
