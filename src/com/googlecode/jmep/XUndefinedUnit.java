/*
 * JMEP - Java Mathematical Expression Parser.
 * Copyright (C) 1999  Jo Desmet
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * You can contact the Original submitter of this library by
 * email at: Jo_Desmet@yahoo.com.
 * 
 */

package com.googlecode.jmep;

/**
 * This is an exception that occurs on access of an Undefined Unit.
 * @author Jo Desmet
 * @see com.iabcinc.jmep.XExpression
 */
public class XUndefinedUnit extends XExpression {
  private static final long serialVersionUID = 1L;
  private String m_sName;

  /*
   * NOTE: The constructor should not defined public as it should only
   * be used within the package.
   */
  public XUndefinedUnit(int iPosition,String sName) {
    super(iPosition,"Undefined Unit: " + sName);
    m_sName = sName;
  }

  /**
   * Gets the name of the Undefined Unit.
   * @return the name of the Undefined Unit.
   */
  public String getUnitName() {
    return m_sName;
  }
}