package test;


/**
 * @author vjayacha
 *
 */
public enum FileAccessModeEnumeration
{
  READ("r"), READ_WRITE("rw"), READ_WRITE_SYNCHRONISE("rws");

  private String mode;

  private FileAccessModeEnumeration(String mode) 
  {
    this.mode = mode;
  }

  /**
   * @return the mode
   */
  public String getMode()
  {
    return mode;
  }
 
}
