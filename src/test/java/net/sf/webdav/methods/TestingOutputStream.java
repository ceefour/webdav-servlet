package net.sf.webdav.methods;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;

import org.junit.Ignore;

@Ignore
public class TestingOutputStream extends ServletOutputStream {

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public void write(int i) throws IOException {
        baos.write(i);
    }

    public String toString() {
        return baos.toString();
    }
}
