/*
 * jIRCd - Java Internet Relay Chat Daemon
 * Copyright 2003 Tyrel L. Haveman <tyrel@haveman.net>
 *
 * This file is part of jIRCd.
 *
 * jIRCd is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * jIRCd is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with jIRCd; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

package jircd.auth;

import java.io.Serializable;
import java.security.Principal;

/**
 * Principal representing IRC operators.
 * @author markhale
 */
public final class OperatorPrincipal implements Principal, Serializable {
    static final long serialVersionUID = -933019140505382763L;
    private final String name;
    
    public OperatorPrincipal(String name) {
        this.name = name;
    }
    public String getName() {
        return name;
    }
    public boolean equals(Object o) {
        return (o != null) && (o instanceof OperatorPrincipal) && name.equals(((OperatorPrincipal)o).name);
    }
    public int hashCode() {
        return name.hashCode();
    }
    public String toString() {
        return name;
    }
}
