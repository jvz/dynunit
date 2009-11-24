/**
 * Copyright 2009 ATG DUST Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package atg.adapter.gsa;

import java.sql.Connection;
import java.sql.SQLException;

import javax.transaction.InvalidTransactionException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

class DoInAutoCommit {

  private final GSARepositorySchemaGenerator mAutoCommit;
  GSARepository mRepository = null;
  Logger mLogger = Logger.getLogger(this.getClass());

  /**
   * Creates a new DoInAutoCommit which operates on the given repository.
   * This class is used to allow code to be executed with autoCommit=true on
   * its database connection. This class will suspend the current transaction
   * if any before setting autoCommit=true. The transaction is resumed and
   * autoCommit is returned to its original state after work is performed.
   * 
   * @param pRepository
   * @param pGsaRepositorySchemaGenerator TODO
   */
  public DoInAutoCommit(GSARepositorySchemaGenerator pGsaRepositorySchemaGenerator, GSARepository pRepository) {
    mAutoCommit = pGsaRepositorySchemaGenerator;
    mRepository = pRepository;
  }

  /**
   * Executes the given work using the connections and logging of the
   * repository passed into the constructor of this class. Returns true if the
   * work was competed without any exceptions.
   * 
   * @param pWork
   */
  public boolean doInAutoCommit(AutoCommitable pWork) {
    Transaction suspended = null;
    boolean success = false;
    try {
      // Suspend the Current Transaction so we can set autoCommit=true
      // Otherwise MSSQL will hang
      suspended = mRepository.getTransactionManager().suspend();
      Connection c = mRepository.getConnection();
      if (mLogger.isDebugEnabled())
        mLogger.log(Priority.DEBUG,"autoCommit = " + c.getAutoCommit()
            + " connection=" + c);
      boolean savedAutoCommit = c.getAutoCommit();
      if (mLogger.isDebugEnabled())
        mLogger.log(Priority.DEBUG,"Setting auto commit = true on connection " + c);
      c.setAutoCommit(true);
      try {
        pWork.doInAutoCommit(c);
        success = true;
      } finally {
        if (c != null) {
          if (mLogger.isDebugEnabled())
            mLogger.log(Priority.DEBUG,"Reverting autoCommit back to "
                + savedAutoCommit);
          c.setAutoCommit(savedAutoCommit);
        }
        if (suspended != null) {
          try {
            mRepository.getTransactionManager().resume(suspended);
          } catch (InvalidTransactionException e) {
            if (mRepository.isLoggingError())
              mLogger.log(Priority.ERROR,e);
          } catch (IllegalStateException e) {
            if (mRepository.isLoggingError())
              mLogger.log(Priority.ERROR,e);
          }
        }
      }
    } catch (SystemException e) {
      if (mRepository.isLoggingError())
        mLogger.log(Priority.ERROR,e);
    } catch (SQLException e) {
      if (mRepository.isLoggingError())
        mLogger.log(Priority.ERROR,e);
    }
    return success;
  }
}