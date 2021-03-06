/**
 * 
 */

package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.CharacterSetEnumeration;

/**
 * @author vjayacha
 * 
 */
public class LargeFileReader
{

  Logger logger = LoggerFactory.getLogger(LargeFileReader.class);
  
  /**
   * 
   * A disk controller moves data in fixed-size blocks. The block sizes used are usually in powers of 2 to simplify addressing. 
   * <p>
   * Operating systems divide their memory address spaces into memory-pages. Memory pages are always multiples of the disk block size.
   * <p>
   * Buffers are therefore initialised with an initial capacity, that's typically a memory-page size.
   */
  public static final int BLOCK_SIZE = 8192;
  
  public final String NEW_LINE = System.getProperty("line.separator");

  /**
   * The entire file whose reference is passed is locked exclusively so that no other locks be in effect simultaneously.
   * <p>
   * File locks are either advisory or mandatory. Advisory locks provide information about current locks to those processes that ask, but such locks
   * are not enforced by the operating system. It is up to the processes involved to cooperate and pay attention to the advice the locks represent.
   * Most Unix and Unix-like operating systems provide advisory locking. Some can also do mandatory locking or a combination of both.
   * <p>
   * Mandatory locks are enforced by the operating system and/or the file-system and will prevent processes, whether they are aware of the locks or
   * not, from gaining access to locked areas of a file. Usually, Microsoft operating systems do mandatory locking. It's wise to assume that all locks
   * are advisory and to use file locking consistently across all applications accessing a common resource. Assuming that all locks are advisory is
   * the only workable cross-platform strategy. Any application depending on mandatory file-locking semantics is inherently non-portable.
   * 
   * 
   * @param file
   *          Reference to the file that has to be locked.
   * @return
   */
  public void lockRead(File file)
  {
    
    if(file == null)
    {
      final String MSG = "A non-null instance of file object is necessary to lock the file!";
      logger.error(MSG);
      throw new RuntimeException(MSG);
    }
    
    try
    {
      if (!file.isFile())
      {
        final String MSG = "File object's instance does not denote a normal file!";
        logger.error(MSG);
        throw new RuntimeException(MSG);
      }

      /**
       * Obtain a file channel for the file whose reference is passed
       */
      FileChannel channel = new RandomAccessFile(file, FileAccessModeEnumeration.READ_WRITE_SYNCHRONISE.getMode()).getChannel();
      FileLock lock = null;

      /*
       * Try to acquire a lock without blocking.
       */
      try
      {
        lock = channel.tryLock();
        
        if (lock == null)
        {
          final String MSG = "A lock could not be acquired for the file with name '" + file.getName() + "' because another program is holding an overlapping lock!";
          logger.error(MSG);
          throw new RuntimeException(MSG);
        }
        else
        {
          logger.info("The file '" + file.getName() + "' was locked successfully!");
          logger.info("Reading the file's content ...");
          
          readLargeTextFile(channel);
        }
      }
      catch (OverlappingFileLockException oflEx)
      {
        final String MSG = "File is already locked in this thread or virtual machine";
        throw new RuntimeException(MSG, oflEx);
      }
      catch (IOException ioEx)
      {
        logger.error(ioEx.getLocalizedMessage(), ioEx);
        throw new RuntimeException(ioEx);
      }
      catch(Exception ex)
      {
    	  logger.error(ex.getLocalizedMessage(),ex);
      }
      finally
      {
        /*
         * Release the lock and close the file
         */
        try
        {
          lock.release();
          channel.close();
          logger.info("Released the lock and closed the file");
        }
        catch (IOException ioEx)
        {
          final String MSG = "Error encountered while trying to release the lock and close the file!";
          logger.error(MSG);
          throw new RuntimeException(MSG, ioEx);
        }
      }

    }
    catch (FileNotFoundException fnfEx)
    {
      final String MSG = "File "+file.getAbsolutePath()+" could not be found on filesystem!";
      logger.error(MSG);
      throw new RuntimeException(MSG, fnfEx);
    }
  }
  
  
  /**
   * Reads a large text file by mapping parts of it into physical memory and 
   * reading it directly by avoiding expensive copy operations between kernel and user space.
   * <p>
   * Memory-mapped file allows one to pretend that the entire file is in memory there by boosting 
   * the programs's performance significantly. 
   * 
   * @param fileChannel   Channel of a random access file.
   * @throws IOException 
   */
  public void readLargeTextFile(FileChannel fileChannel) throws IOException
  {
    
	if(fileChannel == null)
    {
      final String MSG = "A non-null instance of file channel is necessary to read the file!";
      logger.error(MSG);
      throw new RuntimeException(MSG);
    }
    
	long fileLength = fileChannel.size();
	logger.debug("Length of the file measure in bytes = "+fileLength);
	
    System.out.println("\n ***** Content of the file ***** \n"); 
    
    StringBuffer residual = new StringBuffer();
    boolean finalBlockContent = false;
    
	for(long position=0; position<fileLength; position+=BLOCK_SIZE)
    {
    	
    	long size;
    	if(position+BLOCK_SIZE< fileLength) 
    		size = BLOCK_SIZE;
    	else 
    	{
    		size = fileLength-position;
    		finalBlockContent = true;
    	}
    		
    	
    	/**
         * Produce a MappedByteBuffer from the channel, which is a particular kind of direct buffer.
         * Specify the starting point and the length of the region that you want to map in the file; 
         * this means that you have the option to map smaller regions of a large file.
         * 
         *  The file created is 1 MB long. It appears to be accessible all at once because only 
         *  portions of it are brought into memory, and other parts are swapped out. This way, a  
         *  large file (up to 2 GB) can easily be modified. Note that the file-mapping facilities 
         *  of the underlying operating system are used to maximise performance. 
         */
        MappedByteBuffer mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, position, size);
        
        /*
         * mappedByteBuffer will have a position of zero and a limit and capacity of size; its mark will be undefined. 
         */
        
        byte[] destinationByteArray = new byte[mappedByteBuffer.limit()];
        
    	mappedByteBuffer.get(destinationByteArray);
    	String blockContent = convert(destinationByteArray, CharacterSetEnumeration.UTF8);
    	blockContent = blockContent.trim();
    	
    	// Prepend the residual string if it exists
    	if(!StringUtils.isBlank(residual.toString()))
    	{
    		blockContent = residual+blockContent;
    		residual.delete(0, residual.length());
    	}
    		
    		
    	
    	if(  !blockContent.endsWith(System.getProperty("line.separator")) && !finalBlockContent ) 
    	{
    		int lastNewLineIndex = StringUtils.lastIndexOf(blockContent, System.getProperty("line.separator"));
    		
    		String contentAfterLastLineSperator = StringUtils.substring(blockContent, lastNewLineIndex);
    		
    		if( !StringUtils.isBlank( contentAfterLastLineSperator )  )
    		{
    			// Save the string after last line separator
    			residual.append(contentAfterLastLineSperator);
    			// remove the residual string from block content
    			blockContent = StringUtils.removeEnd(blockContent, contentAfterLastLineSperator);
    		}

    	}
    	
    	//logger.info("\n***** BLOCK CONTENT *****\n"+blockContent); 

    	List<String> lines = tokenise(blockContent);
    	
    	// Printing the content of each line to proove this works!
    	logger.info("********** Content of 8 kb block printed one line at a time. **********");
    	for(String line: lines) logger.debug(line);
    	//TODO: Obtain the lines of textual content for each block of data and process it further according to your needs.
    	// Feel free to adapt this - One approach is to process these blocks of lines in parallel instead of sequential processing 
    }
	logger.info("********** End of file's content **********");      
  }
  

  /**
   * Converts an array of bytes into a string with the indicated character set enumeration
   * @param array
   * @param charsetName
   * @return
   */
  public static String convert(byte[] array, CharacterSetEnumeration charsetName)
  {
    if(array == null || array.length==0)
    {
      System.err.println("As the input byte array is empty for conversion, empty string will be returned.");
      return new String();
    }
    else
      return new String(array, getCharacterSet(charsetName));
  }
  
  
  /**
   * Retrieves the corresponding character set given its enumeration name 
   * 
   * @param charsetName
   * @return
   */
  public static Charset getCharacterSet(CharacterSetEnumeration charsetName)
  {
    Charset charset;
    if(charsetName == null)
    {
      System.err.println("As the character set is not specified, UTF-16 shall be used by default.");
      charset = Charset.forName(CharacterSetEnumeration.UTF16BE.getCharset());
    }
    else
      charset = Charset.forName(charsetName.getCharset());
    return  charset;
  }
    
  
  /**
   * Splits a string into lines
   * @param stringToSplit	String contains a set of lines of textual content
   * @return				List of Strings where each string is a line. 
   */
  private List<String> tokenise(String stringToSplit)
  {
	  List<String> lines = new ArrayList<String>();
	  
	  if(StringUtils.isBlank(stringToSplit)) return lines;
	  // Use regular expression representing new lines for Unix and Windows to split.
	  String[] strArray =stringToSplit.split("\\r?\\n");
	  
	  if(strArray == null || strArray.length==0)
		  return lines;
	  else
		  return Arrays.asList(strArray);
  }
  
  
}
  
  
  
