package cc.tonyhook.carambola.backend.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;

public class CarambolaCachedBodyServletOutputStream extends ServletOutputStream {

    private ByteArrayOutputStream cachedBodyOutputStream;
    private ServletOutputStream originalOutputStream;

    public CarambolaCachedBodyServletOutputStream(ServletOutputStream out) {
        this.cachedBodyOutputStream = new ByteArrayOutputStream();
        this.originalOutputStream = out;
    }

    public ByteArrayOutputStream getOutputStream() {
        return cachedBodyOutputStream;
    }

    @Override
    public void write(int b) throws IOException {
        cachedBodyOutputStream.write(b);
        originalOutputStream.write(b);
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener listener) {
        throw new UnsupportedOperationException();
    }

}
