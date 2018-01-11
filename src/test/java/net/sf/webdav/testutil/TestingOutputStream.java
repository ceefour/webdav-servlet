package net.sf.webdav.testutil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

public class TestingOutputStream extends ServletOutputStream {

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    public void write(int i) throws IOException {
        baos.write(i);
    }

    public String toString() {
        return baos.toString();
    }

    /**
     * This method can be used to determine if data can be written without blocking.
     *
     *  @see ServletOutputStream.isReady()
     */
	@Override
	public boolean isReady() {
		return false;
	}

	/**
	 * 
	 * @see ServletOutputStream.setWriteListener()
	 */
	@Override
	public void setWriteListener(WriteListener writeListener) {
		// TODO Auto-generated method stub	
	}

}
