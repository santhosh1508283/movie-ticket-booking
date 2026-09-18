package com.dmg.movieticket.exception;

public enum ErrorCode {

    RESOURCE_NOT_FOUND,
    DUPLICATE_RESOURCE,
    INVALID_REQUEST,
    INVALID_STATE,

    SEAT_NOT_AVAILABLE,
    HOLD_EXPIRED,
    PAYMENT_FAILED,
    CANCELLATION_NOT_ALLOWED
}