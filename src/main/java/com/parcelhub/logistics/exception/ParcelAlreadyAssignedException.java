package com.parcelhub.logistics.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_ACCEPTABLE)
public class ParcelAlreadyAssignedException extends RuntimeException {
	public ParcelAlreadyAssignedException(String message) {
		super(message);
	}
}
