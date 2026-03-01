package com.api_shield.api_shield.interceptor;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class CachedBodyHttpServletResponse extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream capture;
    private PrintWriter writer;

    public CachedBodyHttpServletResponse(HttpServletResponse response) {
        super(response);
        capture = new ByteArrayOutputStream();
    }

    @Override
    public PrintWriter getWriter() {
        writer = new PrintWriter(capture);
        return writer;
    }

    public String getCapturedBody() {
        if (writer != null) {
            writer.flush();
        }
        return capture.toString();
    }
}