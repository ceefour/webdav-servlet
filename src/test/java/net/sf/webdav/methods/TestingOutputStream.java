package net.sf.webdav.methods;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.junit.Ignore;

@Ignore
public class TestingOutputStream extends ServletOutputStream {

    private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Override
    public void write(int i) throws IOException {
        baos.write(i);
    }

    @Override
    public String toString() {
        return baos.toString();
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener arg0) {

    }
}
