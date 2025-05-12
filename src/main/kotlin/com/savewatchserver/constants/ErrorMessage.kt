package com.savewatchserver.constants

object ErrorMessage {
    const val UNAUTHORIZED = "Missing userId in token"
    const val MISSING_CHILD_ID = "Missing childId parameter"
    const val MISSING_DEVICE_ID = "Missing deviceId parameter"
    const val USER_NOT_FOUND = "User not found"
    const val CHILD_NOT_FOUND = "Child not found"
    const val DEVICE_NOT_FOUND = "Device not found"
    const val INVALID_DATA_FORMAT = "Invalid data format"
    const val NAME_CANNOT_BE_BLANK = "Name cannot be blank"
    const val EMAIL_ALREADY_REGISTERED = "Email is already registered"
    const val PASSWORDS_DO_NOT_MATCH = "Passwords do not match"
    const val INVALID_PASSWORD = "Invalid password"
    const val REGISTRATION_FAILED = "Registration failed"
    const val LOGIN_FAILED = "Login failed"
    const val PROFILE_NOT_FOUND = "User profile not found"
    const val DEVICE_ALREADY_LINKED = "Device is already linked to a child"
    const val NO_CHILD_DEVICES_FOUND = "No child devices found for this account"
    const val DEVICE_CONFIRMATION_FAILED = "Failed to confirm device"
    const val DEVICE_CANCELLATION_FAILED = "Failed to cancel device request"
    const val CANNOT_CANCEL_CONFIRMED_DEVICE = "Cannot cancel a confirmed device"
    const val DEVICE_ALREADY_CANCELLED = "The device has already been cancelled"
    const val DEVICE_ALREADY_CONFIRMED = "The device has already been confirmed"
    const val NO_FILE_UPLOADED = "No valid file found in the request"
    const val INTERNAL_SERVER_ERROR = "Internal server error 500"

}