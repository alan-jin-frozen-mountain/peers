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

import net.sourceforge.peers.sip.RFC3261;

public class NonInviteClientTransactionStateCompleted extends
        NonInviteClientTransactionState {

    public NonInviteClientTransactionStateCompleted(String id,
            NonInviteClientTransaction nonInviteClientTransaction) {
        super(id, nonInviteClientTransaction);
        int delay = 0;
        if (RFC3261.TRANSPORT_UDP.equals(nonInviteClientTransaction.transport)) {
            delay = RFC3261.TIMER_T4;
        }
        nonInviteClientTransaction.timer.schedule(nonInviteClientTransaction.new TimerK(), delay);
    }

    @Override
    public void timerKFires() {
        NonInviteClientTransactionState nextState = nonInviteClientTransaction.TERMINATED;
        nonInviteClientTransaction.setState(nextState);
        log(nextState);
    }
    
}
