/**
 * 
 */
package test;


/**
 * @author vjayacha
 *
 */
public enum CharacterSetEnumeration
{
  ASCII("US-ASCII"), ISO("ISO-8859-1"), UTF8("UTF-8"), UTF16("UTF-16"), UTF16BE("UTF-16BE"), UTF16LE("UTF-16LE");

  private String charset;

  private CharacterSetEnumeration(String charset) 
  {
      this.charset = charset;
  }

  public String getCharset()
  {
    return charset;
  }
  
}
