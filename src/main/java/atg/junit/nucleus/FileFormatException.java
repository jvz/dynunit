package atg.junit.nucleus;

import atg.core.exception.ContainerException;

/** A general utility exception thrown if a file is formatted in an unexpected manner.
 *
 * @author marty frenzel
 * @version 1.0
 */
public class FileFormatException
    extends ContainerException
{
  /** no arg constructor */
  public FileFormatException() { super(); }

  /* constructor */
  public FileFormatException( String pMsg ) {
      super( pMsg ); }

  /** constructor */
  public FileFormatException( Throwable pErr ) {
      super( pErr ); }

  /** constructor */
  public FileFormatException( String pMsg, Throwable pErr ) {
      super( pMsg, pErr ); }
}
