/*
    This file is part of Peers.

    Peers is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    Peers is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Foobar; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
    
    Copyright 2007 Yohann Martineau 
*/

package net.sourceforge.peers.sip.transaction;

import net.sourceforge.peers.sip.AbstractState;

public abstract class InviteServerTransactionState extends AbstractState {

    protected InviteServerTransaction inviteServerTransaction;
    
    public InviteServerTransactionState(String id,
            InviteServerTransaction inviteServerTransaction) {
        super(id);
        this.inviteServerTransaction = inviteServerTransaction;
    }

    public void start() {}
    public void receivedInvite() {}
    public void received101To199() {}
    public void transportError() {}
    public void received2xx() {}
    public void received300To699() {}
    public void timerGFires() {}
    public void timerHFiresOrTransportError() {}
    public void receivedAck() {}
    public void timerIFires() {}
    
}
