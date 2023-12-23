package com.example.userservice.exceptions;

public class TokenExpiredException extends  Exception
{

    public TokenExpiredException(String message)
    {
        super(message);
    }
}
