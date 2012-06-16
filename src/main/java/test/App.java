package test;

import java.io.File;
import java.io.IOException;

/**
 * Reads the content of the large file by locking it during the read operation
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException
    {
      LargeFileReader handler = new LargeFileReader();
      
      File largeFile = new File("src/main/resources/test.txt");
      
      handler.lockRead(largeFile);
      
    }
}
