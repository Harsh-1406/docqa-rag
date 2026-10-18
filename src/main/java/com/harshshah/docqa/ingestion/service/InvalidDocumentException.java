package com.harshshah.docqa.ingestion.service;

public class InvalidDocumentException extends RuntimeException {
    public InvalidDocumentException(String message) { super(message); }
}