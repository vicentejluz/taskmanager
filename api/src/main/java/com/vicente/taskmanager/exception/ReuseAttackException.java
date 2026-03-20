package com.vicente.taskmanager.exception;

public class ReuseAttackException extends RuntimeException{
    public ReuseAttackException(String message){
        super(message);
    }
}
