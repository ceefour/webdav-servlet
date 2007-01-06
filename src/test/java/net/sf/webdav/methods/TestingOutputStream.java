package net.sf.webdav.methods;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;

public class TestingOutputStream extends ServletOutputStream {

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    public void write(int i) throws IOException {
        baos.write(i);
    }

    public String toString() {
        return baos.toString();
    }
}
