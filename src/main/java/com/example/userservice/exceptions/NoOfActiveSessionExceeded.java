package com.example.userservice.exceptions;

public class NoOfActiveSessionExceeded extends  Exception
{

    public NoOfActiveSessionExceeded(String message)
    {
        super(message);
    }
}
