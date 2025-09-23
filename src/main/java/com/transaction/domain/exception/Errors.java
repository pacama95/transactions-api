package com.transaction.domain.exception;

public interface Errors {

    interface CreateTransactionsErrors {
        String ERROR_PREFIX = "01%s";

        Error GENERAL_ERROR = new Error(ERROR_PREFIX.formatted("001"));
        Error PUBLISH_DOMAIN_EVENT_ERROR = new Error(ERROR_PREFIX.formatted("002"));
        Error PERSISTENCE_ERROR = new Error(ERROR_PREFIX.formatted("003"));
        Error NOT_FOUND = new Error(ERROR_PREFIX.formatted("004"));
        Error INVALID_INPUT = new Error(ERROR_PREFIX.formatted("005"));
    }

    interface DeleteTransactionsErrors {
        String ERROR_PREFIX = "06%s";

        Error INVALID_INPUT = new Error(ERROR_PREFIX.formatted("01"));
        Error NOT_FOUND = new Error(ERROR_PREFIX.formatted("02"));
        Error PERSISTENCE_ERROR = new Error(ERROR_PREFIX.formatted("03"));
    }

    interface UpdateTransactionsErrors {
        String ERROR_PREFIX = "07%s";

        Error INVALID_INPUT = new Error(ERROR_PREFIX.formatted("01"));
        Error NOT_FOUND = new Error(ERROR_PREFIX.formatted("02"));
        Error PERSISTENCE_ERROR = new Error(ERROR_PREFIX.formatted("03"));
    }

    interface GetTransactionsErrors {
        String ERROR_PREFIX = "08%s";

        Error INVALID_INPUT = new Error(ERROR_PREFIX.formatted("01"));
        Error NOT_FOUND = new Error(ERROR_PREFIX.formatted("02"));
        Error PERSISTENCE_ERROR = new Error(ERROR_PREFIX.formatted("03"));
    }

    interface PublishTransactionsErrors {
        String ERROR_PREFIX = "09%s";

        Error PUBLISH_ERROR = new Error(ERROR_PREFIX.formatted("01"));
    }
}
